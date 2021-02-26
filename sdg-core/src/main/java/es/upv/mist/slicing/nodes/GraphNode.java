package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.*;

/**
 * Represents a node in the various graphs ({@link CFG CFG}, {@link PDG PDG} and {@link SDG SDG}),
 * including its AST representation and the connections it has to other nodes in the same graph.
 * It can hold a string of characters that will be used to represent it. <br/>
 * It is immutable.
 * @param <N> The type of the AST represented by this node.
 */
public class GraphNode<N extends Node> implements Comparable<GraphNode<?>> {
    /** A unique id within the graph. */
    protected final long id;
    /** The textual representation of the node. */
    protected final String label;
    /** The JavaParser AST node represented by this node. */
    protected final N astNode;
    /** A sorted list of actions (usages, definitions and declarations) performed in this node. */
    protected final List<VariableAction> variableActions;
    /** The method calls contained  */
    protected final List<Resolvable<? extends ResolvedMethodLikeDeclaration>> methodCalls = new LinkedList<>();

    /** Create a graph node, with id and variable actions generated automatically. */
    public GraphNode(String label, N astNode) {
        this(IdHelper.getInstance().getNextId(), label, astNode);
    }

    /** Create a graph node, with variable actions generated automatically. */
    protected GraphNode(long id, String label, N astNode) {
        this(id, label, astNode, new LinkedList<>());
        extractVariables();
    }

    /** Create a graph node, with id generated automatically. */
    public GraphNode(String label, N astNode, List<VariableAction> variableActions) {
        this(IdHelper.getInstance().getNextId(), label, astNode, variableActions);
    }

    protected GraphNode(long id, String label, N astNode, List<VariableAction> variableActions) {
        this.id = id;
        this.label = label;
        this.astNode = astNode;
        this.variableActions = variableActions;
    }

    /** Search for all the declarations, definitions and usages in this node. */
    protected void extractVariables() {
        new VariableVisitor().startVisit(this);
    }

    /** A unique id in this graph. */
    public long getId() {
        return id;
    }

    /** The AST node represented by this graph. */
    public N getAstNode() {
        return astNode;
    }

    /** An unmodifiable list of variable actions in this node. */
    public List<VariableAction> getVariableActions() {
        return Collections.unmodifiableList(variableActions);
    }

    /** The node's label. It represents the portion of the node that
     *  is covered by this node, in the case of block statements. */
    public String getLabel() {
        return label;
    }

    /** The node's long-form label, including its id and information on variables. */
    public String getLongLabel() {
        String label = getId() + ": " + getLabel();
        if (!getVariableActions().isEmpty())
            label += "\n" + getVariableActions().stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("--");
        return label;
    }

    // =============================================================
    // ===================  Variables and Calls  ===================
    // =============================================================

    /** Whether this node contains the given call AST node. */
    public boolean containsCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        return methodCalls.stream()
                .anyMatch(callInMethod -> ASTUtils.equalsWithRangeInCU((Node) callInMethod, (Node) call));
    }

    /** Append or prepend the given set of actions to the actions of the given call. */
    public void addActionsForCall(Set<VariableAction.Movable> actions, Resolvable<? extends ResolvedMethodLikeDeclaration> call, boolean prepend) {
        for (int i = 0; i < variableActions.size(); i++) {
            VariableAction var = variableActions.get(i);
            if (var instanceof VariableAction.CallMarker) {
                VariableAction.CallMarker marker = (VariableAction.CallMarker) var;
                if (marker.getCall().equals(call) && marker.isEnter() == prepend) {
                    variableActions.addAll(prepend ? i + 1 : i, actions);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Could not find markers for " + call.resolve().getSignature() + " in " + this);
    }

    /** Append the given actions to after the actions of the given call. */
    public void addActionsAfterCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call, VariableAction... actions) {
        for (int i = 0; i < variableActions.size(); i++) {
            VariableAction var = variableActions.get(i);
            if (var instanceof VariableAction.CallMarker) {
                VariableAction.CallMarker marker = (VariableAction.CallMarker) var;
                if (marker.getCall().equals(call) && !marker.isEnter()) {
                    variableActions.addAll(i + 1, List.of(actions));
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Could not find markers for " + call.resolve().getSignature() + " in " + this);
    }

    /** Create and append a declaration of a variable to the list of actions of this node. */
    public void addDeclaredVariable(Expression variable, String realName) {
        variableActions.add(new VariableAction.Declaration(variable, realName, this));
    }

    /** Create and append a definition of a variable to the list of actions of this node. */
    public void addDefinedVariable(Expression variable, String realName, Expression expression) {
        VariableAction.Definition def = new VariableAction.Definition(variable, realName, this, expression);
        variableActions.add(def);
    }

    /** Create and append a usage of a variable to the list of actions of this node. */
    public void addUsedVariable(Expression variable, String realName) {
        VariableAction.Usage use = new VariableAction.Usage(variable, realName, this);
        variableActions.add(use);
    }

    /** Create and append a call marker to the list of actions of this node. */
    public void addCallMarker(Resolvable<? extends ResolvedMethodLikeDeclaration> call, boolean enter) {
        if (enter) methodCalls.add(call);
        variableActions.add(new VariableAction.CallMarker(call, this, enter));
    }

    /** Create and append a movable variable action to the list of actions of this node. */
    public void addMovableVariable(VariableAction.Movable movable) {
        variableActions.add(movable);
    }

    // ============================================================
    // =======================  Overridden  =======================
    // ============================================================

    @Override
    public String toString() {
        return String.format("%s{id: %s, label: '%s', astNodeType: %s}",
                getClass().getSimpleName(),
                getId(),
                getLabel(),
                getAstNode().getClass().getSimpleName()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        return o instanceof GraphNode && Objects.equals(getId(), ((GraphNode<?>) o).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public int compareTo(GraphNode<?> o) {
        return Long.compare(id, o.id);
    }
}
