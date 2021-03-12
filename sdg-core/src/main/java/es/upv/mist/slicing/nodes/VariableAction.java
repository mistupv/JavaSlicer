package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.DataDependencyArc;
import es.upv.mist.slicing.graphs.Graph;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.graphs.jsysdg.JSysPDG;
import es.upv.mist.slicing.graphs.pdg.PDG;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static es.upv.mist.slicing.nodes.VariableAction.DeclarationType.*;

/** An action upon a variable (e.g. usage, definition, declaration) */
public abstract class VariableAction {
    public enum DeclarationType {
        FIELD,
        STATIC_FIELD,
        PARAMETER,
        LOCAL_VARIABLE,
        SYNTHETIC;

        public static DeclarationType valueOf(ResolvedValueDeclaration resolved) {
            if (resolved.isType())
                return STATIC_FIELD;
            if (resolved.isField() && resolved.asField().isStatic())
                return STATIC_FIELD;
            if (resolved.isField())
                return FIELD;
            if (resolved.isParameter())
                return PARAMETER;
            if (resolved.isVariable())
                return LOCAL_VARIABLE;
            if (resolved instanceof JavaParserSymbolDeclaration)
                return LOCAL_VARIABLE;
            throw new IllegalArgumentException("Invalid resolved value declaration");
        }

        public static DeclarationType valueOf(Expression expression) {
            if (expression instanceof ThisExpr || expression instanceof SuperExpr)
                return FIELD;
            else if (expression instanceof NameExpr)
                try {
                    return valueOf(expression.asNameExpr().resolve());
                } catch (UnsolvedSymbolException e) {
                    return STATIC_FIELD;
                }
            else if (expression instanceof FieldAccessExpr)
                return valueOf(expression.asFieldAccessExpr().getScope());
            else
                throw new IllegalStateException("Invalid expression type");
        }
    }

    protected final String name;
    protected final DeclarationType declarationType;

    protected GraphNode<?> graphNode;
    protected ObjectTree objectTree;
    protected boolean optional = false;

    /// Variables that control the automatic connection of ObjectTrees between VariableAction
    /** A list of pairs representing connections to be made between trees in the PDG.
     *  The variable action that contains the tree we must connect to in the PDG.
     *  The string, or member where the tree connection must start (in PDG). E.g.: our tree is "a.b.c" and this variable is "a",
     *  the members "a.b" and "a.b.c" will be connected to "b" and "b.c" in treeConnectionTarget's tree.. */
    protected final List<PDGConnection> pdgTreeConnections = new LinkedList<>();

    private VariableAction(DeclarationType declarationType, String name, GraphNode<?> graphNode) {
        this(declarationType, name, graphNode, null);
    }

    private VariableAction(DeclarationType declarationType, String name, GraphNode<?> graphNode, ObjectTree objectTree) {
        assert name != null && !name.isEmpty();
        this.declarationType = declarationType;
        this.name = name;
        this.graphNode = graphNode;
        this.objectTree = objectTree;
    }

    // ======================================================
    // ================= BASIC GETTERS/SETTERS ==============
    // ======================================================

    public boolean isParameter() {
        return PARAMETER == declarationType;
    }

    public boolean isField() {
        return declarationType == FIELD || declarationType == STATIC_FIELD;
    }

    public boolean isStatic() {
        return declarationType == STATIC_FIELD;
    }

    public boolean isLocalVariable() {
        return declarationType == LOCAL_VARIABLE;
    }

    public ObjectTree getObjectTree() {
        if (!hasObjectTree())
            setObjectTree(new ObjectTree(getName()));
        return objectTree;
    }

    protected void setObjectTree(ObjectTree objectTree) {
        this.objectTree = objectTree;
    }

    public String getName() {
        return name;
    }

    /** Whether this action is always performed when its parent node is executed or not. */
    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    /** The node that performs this action, in which this object is contained. */
    public GraphNode<?> getGraphNode() {
        return graphNode;
    }

    /** Whether this action is performed upon an invented variable,
     * introduced by this library (e.g. the active exception or the returned value). */
    public boolean isSynthetic() {
        return declarationType == SYNTHETIC;
    }

    /** Whether the argument is performed upon the same variable as this action. */
    public boolean matches(VariableAction action) {
        return name.equals(action.name);
    }

    public boolean isPrimitive() {
        return isRootAction() && !hasObjectTree();
    }

    // ======================================================
    // =================== OBJECT TREE ======================
    // ======================================================

    public boolean hasTreeMember(String member) {
        if (member.isEmpty())
            return hasObjectTree();
        if (!hasObjectTree())
            return false;
        return getObjectTree().hasMember(member);
    }

    public boolean hasObjectTree() {
        return objectTree != null;
    }

    public void setPDGTreeConnectionTo(VariableAction targetAction, String sourcePrefixWithoutRoot, String targetPrefixWithoutRoot) {
        pdgTreeConnections.add(new ObjectTreeConnection(this, targetAction, sourcePrefixWithoutRoot, targetPrefixWithoutRoot));
    }

    public void setPDGValueConnection(String member) {
        pdgTreeConnections.add(new ValueConnection(this, member));
    }

    public void applyPDGTreeConnections(JSysPDG pdg) {
        pdgTreeConnections.forEach(c -> c.apply(pdg));
    }

    public void applySDGTreeConnection(JSysDG sdg, VariableAction targetAction) {
        ObjectTreeConnection connection = new ObjectTreeConnection(this, targetAction, "", "");
        connection.applySDG(sdg);
    }

    // ======================================================
    // =================== ROOT ACTIONS =====================
    // ======================================================

    public VariableAction getRootAction() {
        assert !isRootAction();
        if (this instanceof Movable) {
            Movable movable = (Movable) this;
            return new Movable(movable.inner.getRootAction(), movable.getRealNode());
        }
        if (this instanceof Usage)
            return new Usage(declarationType, ObjectTree.removeFields(name), graphNode);
        if (this instanceof Definition)
            return new Definition(declarationType, ObjectTree.removeFields(name), graphNode, asDefinition().expression);
        if (this instanceof Declaration)
            throw new UnsupportedOperationException("Can't create a root node for a declaration!");
        throw new IllegalStateException("Invalid action type");
    }

    public boolean isRootAction() {
        return isSynthetic() || Objects.equals(ObjectTree.removeFields(name), name);
    }

    public boolean rootMatches(VariableAction b) {
        return ObjectTree.removeFields(name).equals(ObjectTree.removeFields(b.name));
    }

    // ======================================================
    // ============== SUBTYPES AND CLONING ==================
    // ======================================================

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
        ObjectTree tree = hasObjectTree() ? (ObjectTree) getObjectTree().clone() : null;
        return new Usage(declarationType, name, graphNode, tree);
    }

    /** Creates a new definition action with the same variable and the given node. */
    public final Definition toDefinition(GraphNode<?> graphNode) {
        ObjectTree tree = hasObjectTree() ? (ObjectTree) getObjectTree().clone() : null;
        return new Definition(declarationType, name, graphNode, tree);
    }

    /** Creates a new declaration action with the same variable and the given node. */
    public final Declaration toDeclaration(GraphNode<?> graphNode) {
        ObjectTree tree = hasObjectTree() ? (ObjectTree) getObjectTree().clone() : null;
        return new Declaration(declarationType, name, graphNode, tree);
    }

    public final <A extends VariableAction> A createCopy() {
        return createCopy(null);
    }

    @SuppressWarnings("unchecked")
    public final <A extends VariableAction> A createCopy(GraphNode<?> graphNode) {
        if (this instanceof Usage)
            return (A) toUsage(graphNode);
        if (this instanceof Definition)
            return (A) toDefinition(graphNode);
        if (this instanceof Declaration)
            return (A) toDeclaration(graphNode);
        if (this instanceof Movable) {
            assert graphNode == null || graphNode instanceof SyntheticNode;
            Movable m = (Movable) this;
            return (A) new Movable(m.inner.createCopy(), (SyntheticNode<?>) graphNode);
        }
        throw new IllegalStateException("This kind of variable action can't be copied");
    }

    // ======================================================
    // =============== OVERRIDDEN METHODS ===================
    // ======================================================

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VariableAction &&
                obj.getClass().equals(getClass()) &&
                name.equals(((VariableAction) obj).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), name);
    }

    @Override
    public String toString() {
        return "{" + name + "}";
    }

    // ======================================================
    // ==================== SUBCLASSES ======================
    // ======================================================

    /** An invented action used to locate the relative position of the start and end of a call inside a list of actions. */
    public static class CallMarker extends VariableAction {
        protected final Resolvable<? extends ResolvedMethodLikeDeclaration> call;
        protected final boolean enter;

        public CallMarker(Resolvable<? extends ResolvedMethodLikeDeclaration> call, GraphNode<?> graphNode, boolean enter) {
            super(null, String.format("-%s-%s-", enter ? "call" : "return", call.resolve().getSignature()), graphNode);
            this.call = call;
            this.enter = enter;
        }

        @Override
        public boolean isRootAction() {
            return true;
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
        public Usage(DeclarationType declarationType, String name, GraphNode<?> graphNode) {
            super(Objects.requireNonNull(declarationType), name, graphNode);
        }

        public Usage(DeclarationType declarationType, String name, GraphNode<?> graphNode, ObjectTree objectTree) {
            super(Objects.requireNonNull(declarationType), name, graphNode, objectTree);
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

        public Definition(DeclarationType declarationType, String name, GraphNode<?> graphNode) {
            this(declarationType, name, graphNode, (Expression) null);
        }

        public Definition(DeclarationType declarationType, String name, GraphNode<?> graphNode, Expression expression) {
            super(Objects.requireNonNull(declarationType), name, graphNode);
            this.expression = expression;
        }

        public Definition(DeclarationType declarationType, String name, GraphNode<?> graphNode, ObjectTree objectTree) {
            this(declarationType, name, graphNode, null, objectTree);
        }

        public Definition(DeclarationType declarationType, String name, GraphNode<?> graphNode, Expression expression, ObjectTree objectTree) {
            super(Objects.requireNonNull(declarationType), name, graphNode, objectTree);
            this.expression = expression;
        }

        public void setTotallyDefinedMember(String totallyDefinedMember) {
            this.totallyDefinedMember = Objects.requireNonNull(totallyDefinedMember);
        }

        public boolean isTotallyDefinedMember(String member) {
            if (totallyDefinedMember == null)
                return false;
            if (totallyDefinedMember.equals(member))
                return true;
            if (member.startsWith(totallyDefinedMember)
                    || ObjectTree.removeRoot(member).startsWith(ObjectTree.removeRoot(totallyDefinedMember)))
                return ObjectTree.removeRoot(member).isEmpty() || hasTreeMember(member);
            return false;
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
        public Declaration(DeclarationType declarationType, String name, GraphNode<?> graphNode) {
            super(Objects.requireNonNull(declarationType), name, graphNode);
        }

        public Declaration(DeclarationType declarationType, String name, GraphNode<?> graphNode, ObjectTree objectTree) {
            super(Objects.requireNonNull(declarationType), name, graphNode, objectTree);
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
            super(inner.declarationType, inner.name, inner.graphNode);
            if (inner instanceof Movable)
                throw new IllegalArgumentException("'inner' must be an unmovable action");
            this.realNode = pdgNode;
            this.inner = inner;
        }

        @Override
        public ObjectTree getObjectTree() {
            if (!inner.hasObjectTree())
                inner.setObjectTree(new ObjectTree(getName()));
            return inner.getObjectTree();
        }

        @Override
        protected void setObjectTree(ObjectTree objectTree) {
            inner.objectTree = objectTree;
        }

        @Override
        public boolean hasObjectTree() {
            return inner.objectTree != null;
        }

        /** The final location of this action. This node may not yet be present
         *  in the graph, if {@link #move(Graph)} has not been invoked. */
        public SyntheticNode<?> getRealNode() {
            return realNode;
        }

        /** Move the action from its node to its real node. The real node is added to the graph,
         *  the action is deleted from its original node's list, a copy is created with the real
         *  target and any {@link DataDependencyArc} is relocated to match this change. */
        public VariableAction move(Graph graph) {
            // Add node
            graph.addVertex(realNode);
            // Move to node
            moveOnly();
            // Move data dependencies
            Set.copyOf(graph.edgesOf(graphNode).stream()
                    .filter(Arc::isDataDependencyArc)
                    .map(Arc::asDataDependencyArc)
                    .filter(arc -> arc.getSourceVar() == this || arc.getTargetVar() == this)
                    .collect(Collectors.toSet())) // copying to avoid modifying while iterating
                    .forEach(arc -> moveDataDependencyArc(arc, graph, inner));
            return inner;
        }

        /** Relocate the inner VA from its current node to its real node. */
        public void moveOnly() {
            graphNode.variableActions.remove(this);
            realNode.variableActions.add(inner);
            inner.graphNode = realNode;
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

    public interface PDGConnection {
        void apply(JSysPDG graph);
    }
}
