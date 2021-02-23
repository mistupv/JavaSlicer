package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.DataDependencyArc;
import es.upv.mist.slicing.graphs.Graph;
import es.upv.mist.slicing.graphs.pdg.PDG;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** An action upon a variable (e.g. usage, definition, declaration) */
public abstract class VariableAction {
    protected static final String VARIABLE_PATTERN = "^([a-zA-Z][a-zA-Z0-9_]*|[_a-zA-Z][a-zA-Z0-9_]+" +
            "|([a-zA-Z][a-zA-Z0-9_]*)([\\.][a-zA-Z][a-zA-Z]*)+)$";

    protected final Expression variable;
    protected final GraphNode<?> graphNode;

    protected boolean optional = false;
    protected ResolvedValueDeclaration resolvedVariableCache;

    public VariableAction(Expression variable, GraphNode<?> graphNode) {
        assert variable.isNameExpr() || variable.isFieldAccessExpr();
        this.variable = variable;
        this.graphNode = graphNode;
    }

    /** Whether this action is performed upon an invented variable,
     * introduced by this library (e.g. the active exception or the returned value). */
    public boolean isSynthetic() {
        return !getVariable().matches(VARIABLE_PATTERN);
    }

    public String getVariable() {
        return variable.toString();
    }

    public Expression getVariableExpression() {
        return variable;
    }

    public ResolvedValueDeclaration getResolvedValueDeclaration() {
        if (resolvedVariableCache == null) {
            if (variable.isFieldAccessExpr())
                resolvedVariableCache = variable.asFieldAccessExpr().resolve();
            else if (variable.isNameExpr())
                resolvedVariableCache = variable.asNameExpr().resolve();
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
        return Objects.equals(action.variable, variable)
                // Needed to link FieldAccessExpr with NameExpr in case they refer to the same Field Variable (this.x = x + 1)
                || action.variable.toString().equals(variable.toString());
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
        return new Usage(variable, graphNode);
    }

    /** Creates a new definition action with the same variable and the given node. */
    public final Definition toDefinition(GraphNode<?> graphNode) {
        return new Definition(variable, graphNode);
    }

    /** Creates a new declaration action with the same variable and the given node. */
    public final Declaration toDeclaration(GraphNode<?> graphNode) {
        return new Declaration(variable, graphNode);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VariableAction &&
                obj.getClass().equals(getClass()) &&
                variable.equals(((VariableAction) obj).variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), variable);
    }

    @Override
    public String toString() {
        return "{" + variable + "}";
    }

    /** An invented action used to locate the relative position of the start and end of a call inside a list of actions. */
    public static class CallMarker extends VariableAction {
        protected final Resolvable<? extends ResolvedMethodLikeDeclaration> call;
        protected final boolean enter;

        public CallMarker(Resolvable<? extends ResolvedMethodLikeDeclaration> call, GraphNode<?> graphNode, boolean enter) {
            super(new NameExpr(String.format("-%s-%s-", enter ? "call" : "return", call.resolve().getSignature())), graphNode);
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
        public Usage(Expression variable, GraphNode<?> graphNode) {
            super(variable, graphNode);
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

        public Definition(Expression variable, GraphNode<?> graphNode) {
            this(variable, graphNode, null);
        }

        public Definition(Expression variable, GraphNode<?> graphNode, Expression expression) {
            super(variable, graphNode);
            this.expression = expression;
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
        public Declaration(Expression variable, GraphNode<?> graphNode) {
            super(variable, graphNode);
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
            super(inner.variable, inner.graphNode);
            if (inner instanceof Movable)
                throw new IllegalArgumentException("'inner' must be an unmovable action");
            this.realNode = pdgNode;
            this.inner = inner;
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
            // Create unwrapped action (the graphNode field must be changed).
            VariableAction newAction;
            try {
                if (inner instanceof Definition && inner.asDefinition().getExpression() != null)
                    newAction = inner.getClass().getConstructor(Expression.class, GraphNode.class, Expression.class)
                            .newInstance(variable, realNode, inner.asDefinition().expression);
                else
                    newAction = inner.getClass().getConstructor(Expression.class, GraphNode.class)
                            .newInstance(variable, realNode);
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
