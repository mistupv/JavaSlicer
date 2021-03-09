package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.DataDependencyArc;
import es.upv.mist.slicing.graphs.Graph;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.graphs.jsysdg.JSysPDG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.OutputNode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static es.upv.mist.slicing.graphs.cfg.CFGBuilder.VARIABLE_NAME_OUTPUT;

/** An action upon a variable (e.g. usage, definition, declaration) */
public abstract class VariableAction {
    protected static final String VARIABLE_PATTERN = "([a-zA-Z][a-zA-Z0-9_]*|_[a-zA-Z0-9_]+)";
    protected static final String FIELD_PATTERN = "^" + VARIABLE_PATTERN + "(\\." + VARIABLE_PATTERN + ")*" + "$";

    protected final Expression variable;
    protected final String realName;
    protected final GraphNode<?> graphNode;
    protected final ObjectTree objectTree;

    protected boolean optional = false;
    protected ResolvedValueDeclaration resolvedVariableCache;

    /// Variables that control the automatic connection of ObjectTrees between VariableAction
    /** A list of pairs representing connections to be made between trees in the PDG.
     *  The variable action that contains the tree we must connect to in the PDG.
     *  The string, or member where the tree connection must start (in PDG). E.g.: our tree is "a.b.c" and this variable is "a",
     *  the members "a.b" and "a.b.c" will be connected to "b" and "b.c" in treeConnectionTarget's tree.. */
    protected final List<ObjectTreeConnection> pdgTreeConnections = new LinkedList<>();

    public VariableAction(Expression variable, String realName, GraphNode<?> graphNode) {
        this(variable, realName, graphNode,  new ObjectTree());
    }

    public VariableAction(Expression variable, String realName, GraphNode<?> graphNode, ObjectTree objectTree) {
        assert realName != null && !realName.isEmpty();
        assert objectTree != null;
        this.variable = variable;
        this.realName = realName;
        this.graphNode = graphNode;
        this.objectTree = objectTree;
    }

    public ObjectTree getObjectTree() {
        return objectTree;
    }

    /** Add a field of this object, such that the same action performed on the object
     *  is applied to this field too. Fields of fields may be specified separated by dots. */
    public void addObjectField(String fieldName) {
        getObjectTree().addField(fieldName);
    }

    public void setPDGTreeConnectionTo(VariableAction targetAction, String sourcePrefixWithoutRoot, String targetPrefixWithoutRoot) {
        pdgTreeConnections.add(new ObjectTreeConnection(this, targetAction, sourcePrefixWithoutRoot, targetPrefixWithoutRoot));
    }

    public void applyPDGTreeConnections(JSysPDG pdg) {
        pdgTreeConnections.forEach(c -> c.applyPDG(pdg));
    }

    public void applySDGTreeConnection(JSysDG sdg, VariableAction targetAction) {
        ObjectTreeConnection connection = new ObjectTreeConnection(this, targetAction, "", "");
        connection.applySDG(sdg);
    }

    public VariableAction getRootAction() {
        assert !isRootAction();
        assert variable == null || variable.isNameExpr() || variable.isFieldAccessExpr() || variable.isThisExpr();
        if (this instanceof Movable) {
            Movable movable = (Movable) this;
            return new Movable(movable.inner.getRootAction(), movable.getRealNode());
        }
        Expression nVar;
        String nRealName = getRootVariable();
        GraphNode<?> nNode = graphNode;
        Expression nExpr = isDefinition() ? asDefinition().expression : null;
        if (variable == null || !(variable instanceof FieldAccessExpr)) {
            // This appears only when generated from a field: just set the variable to null
            assert realName.contains(".this.");
            nVar = null;
        } else { // We are in a FieldAccessExpr
            nVar = variable;
            while (nVar.isFieldAccessExpr())
                nVar = variable.asFieldAccessExpr().getScope();
        }
        if (this instanceof Usage)
            return new Usage(nVar, nRealName, nNode);
        if (this instanceof Definition)
            return new Definition(nVar, nRealName, nNode, nExpr);
        if (this instanceof Declaration)
            throw new UnsupportedOperationException("Can't create a root node for a declaration!");
        throw new IllegalStateException("Invalid action type");
    }

    public String getRootVariable() {
        Pattern rootVariable = Pattern.compile("^(?<root>(([_0-9A-Za-z]+\\.)*this)|([_0-9A-Za-z]+)).*$");
        Matcher matcher = rootVariable.matcher(realName);
        if (matcher.matches()) {
            if (matcher.group("root") != null)
                return matcher.group("root"); // [type.this] or [this]
            else
                throw new IllegalStateException("Invalid real name: " + realName);
        } else {
            return null;
        }
    }

    public boolean isRootAction() {
        return isSynthetic() || Objects.equals(getRootVariable(), realName);
    }

    public static boolean typeMatches(VariableAction a, VariableAction b) {
        return (a.isDeclaration() && b.isDeclaration()) ||
                (a.isDefinition() && b.isDefinition()) ||
                (a.isUsage() && b.isUsage());
    }

    public static boolean rootMatches(VariableAction a, VariableAction b) {
        return a.getRootVariable().equals(b.getRootVariable());
    }

    /** Whether this action is performed upon an invented variable,
     * introduced by this library (e.g. the active exception or the returned value). */
    public boolean isSynthetic() {
        return !getVariable().matches(FIELD_PATTERN);
    }

    public boolean isPrimitive() {                                          // if (otherwise) handleAsCallReturn
        if (realName.equals(VARIABLE_NAME_OUTPUT)) {
            if (graphNode.getAstNode() instanceof ReturnStmt) {
                return !ASTUtils.declarationReturnIsObject(ASTUtils.getDeclarationNode(graphNode.getAstNode()));
            } else {
                return ASTUtils.resolvableIsPrimitive(graphNode.locateCallForVariableAction(this).getCall());
            }
        }
        ResolvedValueDeclaration resolved = getResolvedValueDeclaration();  // if (graphNode.getAstNode() instanceof ReturnStmt) handleAsTypeOfCurrentMethod
        return resolved.getType() != null && resolved.getType().isPrimitive();
    }

    public String getVariable() {
        return realName;
    }

    public boolean hasVariableExpression() {
        return variable != null;
    }

    public Expression getVariableExpression() {
        return variable;
    }

    /**
     * Returns the resolved value declaration. When the action being performed
     * is done so on a ThisExpr, the resulting declaration has the following properties:
     * <ul>
     *     <li>Can return type and name</li>
     *     <li>Is not a parameter, it's a field.</li>
     *     <li>All other methods are left to their default implementations.</li>
     * </ul>
     */
    public ResolvedValueDeclaration getResolvedValueDeclaration() {
        if (resolvedVariableCache == null) {
            if (variable instanceof Resolvable) {
                try {
                    var resolved = ((Resolvable<?>) variable).resolve();
                    if (resolved instanceof ResolvedValueDeclaration)
                        resolvedVariableCache = (ResolvedValueDeclaration) resolved;
                } catch (UnsolvedSymbolException ignored) {
                    resolvedVariableCache = new ResolvedValueDeclaration() {
                        @Override
                        public ResolvedType getType() {
                            return null;
                        }

                        @Override
                        public String getName() {
                            return realName;
                        }

                        @Override
                        public boolean isType() {
                            return true;
                        }

                        @Override
                        public boolean isField() {
                            return true;
                        }
                    };
                }
            }
            if (resolvedVariableCache == null)
                resolvedVariableCache = new ResolvedValueDeclaration() {
                    @Override
                    public ResolvedType getType() {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return realName;
                    }

                    @Override
                    public boolean isField() {
                        return true;
                    }
                };
        }
        return resolvedVariableCache;
    }

    // TODO: detected optional actions
    /** Whether this action is always performed when its parent node is executed or not. */
    public boolean isOptional() {
        return optional;
    }

    /** The node that performs this action, in which this object is contained. */
    public GraphNode<?> getGraphNode() {
        return graphNode;
    }

    /** Whether the argument is performed upon the same variable as this action. */
    public boolean matches(VariableAction action) {
        return Objects.equals(action.realName, realName);
    }

    public boolean isUsage() {
        return this instanceof Usage;
    }

    public boolean isDefinition() {
        return this instanceof Definition;
    }

    public boolean isDeclaration() {
        return this instanceof Declaration;
    }

    public Usage asUsage() {
        return (Usage) this;
    }

    public Definition asDefinition() {
        return (Definition) this;
    }

    public Declaration asDeclaration() {
        return (Declaration) this;
    }

    /** Creates a new usage action with the same variable and the given node. */
    public final Usage toUsage(GraphNode<?> graphNode) {
        return new Usage(variable, realName, graphNode, (ObjectTree) getObjectTree().clone());
    }

    /** Creates a new definition action with the same variable and the given node. */
    public final Definition toDefinition(GraphNode<?> graphNode) {
        return new Definition(variable, realName, graphNode, (ObjectTree) getObjectTree().clone());
    }

    /** Creates a new declaration action with the same variable and the given node. */
    public final Declaration toDeclaration(GraphNode<?> graphNode) {
        return new Declaration(variable, realName, graphNode, (ObjectTree) getObjectTree().clone());
    }

    @SuppressWarnings("unchecked")
    public final <A extends VariableAction> A createCopy() {
        if (this instanceof Usage)
            return (A) toUsage(null);
        if (this instanceof Definition)
            return (A) toDefinition(null);
        if (this instanceof Declaration)
            return (A) toDeclaration(null);
        if (this instanceof Movable) {
            Movable m = (Movable) this;
            return (A) new Movable(m.inner.createCopy(), null);
        }
        throw new IllegalStateException("This kind of variable action can't be copied");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VariableAction &&
                obj.getClass().equals(getClass()) &&
                Objects.equals(variable, ((VariableAction) obj).variable) &&
                realName.equals(((VariableAction) obj).realName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), variable, realName);
    }

    @Override
    public String toString() {
        return "{" + realName + "}";
    }

    /** An invented action used to locate the relative position of the start and end of a call inside a list of actions. */
    public static class CallMarker extends VariableAction {
        protected final Resolvable<? extends ResolvedMethodLikeDeclaration> call;
        protected final boolean enter;

        public CallMarker(Resolvable<? extends ResolvedMethodLikeDeclaration> call, GraphNode<?> graphNode, boolean enter) {
            super(null, String.format("-%s-%s-", enter ? "call" : "return", call.resolve().getSignature()), graphNode);
            this.call = call;
            this.enter = enter;
        }

        /** The call this marker represents. */
        public Resolvable<? extends ResolvedMethodLikeDeclaration> getCall() {
            return call;
        }

        /** Whether this is the start marker (true) or the end marker (false). */
        public boolean isEnter() {
            return enter;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CallMarker && super.equals(o) && enter == ((CallMarker) o).enter
                    && Objects.equals(call, ((CallMarker) o).call);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), call, enter);
        }
    }

    /** A usage of a variable. */
    public static class Usage extends VariableAction {
        public Usage(Expression variable, String realName, GraphNode<?> graphNode) {
            super(variable, realName, graphNode);
        }

        public Usage(Expression variable, String realName, GraphNode<?> graphNode, ObjectTree objectTree) {
            super(variable, realName, graphNode, objectTree);
        }

        @Override
        public String toString() {
            return "USE" + super.toString();
        }
    }

    /** A definition of a variable. */
    public static class Definition extends VariableAction {
        /** The value to which the variable has been defined. */
        protected final Expression expression;
        /** The members of the object tree that are total definitions. */
        protected String totallyDefinedMember;

        public Definition(Expression variable, String realName, GraphNode<?> graphNode) {
            this(variable, realName, graphNode, (Expression) null);
        }

        public Definition(Expression variable, String realName, GraphNode<?> graphNode, Expression expression) {
            super(variable, realName, graphNode);
            this.expression = expression;
        }

        public Definition(Expression variable, String realName, GraphNode<?> graphNode, ObjectTree objectTree) {
            this(variable, realName, graphNode, null, objectTree);
        }

        public Definition(Expression variable, String realName, GraphNode<?> graphNode, Expression expression, ObjectTree objectTree) {
            super(variable, realName, graphNode, objectTree);
            this.expression = expression;
        }

        public void setTotallyDefinedMember(String totallyDefinedMember) {
            this.totallyDefinedMember = Objects.requireNonNull(totallyDefinedMember);
        }

        public boolean isTotallyDefinedMember(String member) {
            if (totallyDefinedMember == null)
                return false;
            return totallyDefinedMember.equals(member) || totallyDefinedMember.startsWith(member)
                    || ObjectTree.removeRoot(totallyDefinedMember).startsWith(ObjectTree.removeRoot(member));
        }

        /** @see #expression */
        public Expression getExpression() {
            return expression;
        }

        @Override
        public String toString() {
            return "DEF" + super.toString();
        }
    }

    /** A declaration of a variable. */
    public static class Declaration extends VariableAction {
        public Declaration(Expression variable, String realName, GraphNode<?> graphNode) {
            super(variable, realName, graphNode);
        }

        public Declaration(Expression variable, String realName, GraphNode<?> graphNode, ObjectTree objectTree) {
            super(variable, realName, graphNode, objectTree);
        }

        @Override
        public String toString() {
            return "DEC" + super.toString();
        }
    }

    /**
     * A variable action that is found in a given node, but whose final location will be a different node.
     * When {@link #move(Graph)} is called, the node and its data dependencies are moved to the new node,
     * which is added to the graph.
     */
    public static class Movable extends VariableAction {
        protected final SyntheticNode<?> realNode;
        protected final VariableAction inner;

        /** Create a new movable action, with an inner action that will be used
         * to generate dependencies and a {@link PDG PDG} node that
         * is the final location of this action. */
        public Movable(VariableAction inner, SyntheticNode<?> pdgNode) {
            super(inner.variable, inner.realName, inner.graphNode);
            if (inner instanceof Movable)
                throw new IllegalArgumentException("'inner' must be an unmovable action");
            this.realNode = pdgNode;
            this.inner = inner;
        }

        public ObjectTree getObjectTree() {
            return inner.getObjectTree();
        }

        /** The final location of this action. This node may not yet be present
         *  in the graph, if {@link #move(Graph)} has not been invoked. */
        public SyntheticNode<?> getRealNode() {
            return realNode;
        }

        @Override
        public boolean isPrimitive() {
            if (realName.equals(VARIABLE_NAME_OUTPUT)) {
                if (realNode instanceof CallNode.Return) {
                    return ASTUtils.resolvableIsPrimitive((Resolvable<? extends ResolvedMethodLikeDeclaration>) realNode.getAstNode());
                } else if (realNode instanceof OutputNode) {
                    return !ASTUtils.declarationReturnIsObject(ASTUtils.getDeclarationNode(realNode.getAstNode()));
                }
            }
            return super.isPrimitive();
        }

        /** Move the action from its node to its real node. The real node is added to the graph,
         *  the action is deleted from its original node's list, a copy is created with the real
         *  target and any {@link DataDependencyArc} is relocated to match this change. */
        public VariableAction move(Graph graph) {
            // Create unwrapped action (the graphNode field must be changed).
            VariableAction newAction;
            try {
                if (inner instanceof Definition && inner.asDefinition().getExpression() != null)
                    newAction = inner.getClass().getConstructor(Expression.class, String.class, GraphNode.class, Expression.class, ObjectTree.class)
                            .newInstance(variable, realName, realNode, inner.asDefinition().expression, getObjectTree());
                else
                    newAction = inner.getClass().getConstructor(Expression.class, String.class, GraphNode.class, ObjectTree.class)
                            .newInstance(variable, realName, realNode, getObjectTree());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new UnsupportedOperationException("The VariableAction constructor has changed!", e);
            }
            // Add node
            graph.addVertex(realNode);
            // Move to node
            graphNode.variableActions.remove(this);
            realNode.variableActions.add(newAction);
            // Move data dependencies
            Set.copyOf(graph.edgesOf(graphNode).stream()
                    .filter(Arc::isDataDependencyArc)
                    .map(Arc::asDataDependencyArc)
                    .filter(arc -> arc.getSourceVar() == this || arc.getTargetVar() == this)
                    .collect(Collectors.toSet())) // copying to avoid modifying while iterating
                    .forEach(arc -> moveDataDependencyArc(arc, graph, newAction));
            return newAction;
        }

        /** Relocates a data dependency arc, by creating a new one with matching information and deleting the old one. */
        protected void moveDataDependencyArc(DataDependencyArc arc, Graph graph, VariableAction newAction) {
            if (arc.getSourceVar() == this) {
                graph.addEdge(realNode, graph.getEdgeTarget(arc), new DataDependencyArc(newAction, arc.getTargetVar()));
            } else {
                graph.addEdge(graph.getEdgeSource(arc), realNode, new DataDependencyArc(arc.getSourceVar(), newAction));
            }
            graph.removeEdge(arc);
        }

        @Override
        public boolean isUsage() {
            return inner instanceof Usage;
        }

        @Override
        public boolean isDefinition() {
            return inner instanceof Definition;
        }

        @Override
        public boolean isDeclaration() {
            return inner instanceof Declaration;
        }

        @Override
        public Usage asUsage() {
            return (Usage) inner;
        }

        @Override
        public Definition asDefinition() {
            return (Definition) inner;
        }

        @Override
        public Declaration asDeclaration() {
            return (Declaration) inner;
        }

        @Override
        public String toString() {
            return String.format("%s(%d)", inner.toString(), realNode.getId());
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Movable && super.equals(o) && Objects.equals(realNode, ((Movable) o).realNode) &&
                    Objects.equals(inner, ((Movable) o).inner);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), realNode, inner);
        }
    }

}
