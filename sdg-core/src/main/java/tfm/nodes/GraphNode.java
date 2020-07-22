package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.NameExpr;
import org.jetbrains.annotations.NotNull;
import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
import tfm.nodes.type.NodeType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a node in the various graphs ({@link CFG CFG},
 * {@link PDG PDG} and {@link SDG SDG}),
 * including its AST representation and the connections it has to other nodes
 * in the same graph. It can hold a string of characters that will be used
 * to represent it.
 * <br/>
 * It is immutable.
 * @param <N> The type of the AST represented by this node.
 */
public class GraphNode<N extends Node> implements Comparable<GraphNode<?>> {
    public static final NodeFactory DEFAULT_FACTORY = TypeNodeFactory.fromType(NodeType.STATEMENT);

    protected final NodeType nodeType;

    protected final long id;
    protected final String instruction;
    protected final N astNode;
    protected final List<VariableAction> variableActions;

    protected GraphNode(NodeType type, String instruction, @NotNull N astNode) {
        this(IdHelper.getInstance().getNextId(), type, instruction, astNode);
    }

    protected GraphNode(long id, NodeType type, String instruction, @NotNull N astNode) {
        this(id, type, instruction, astNode, new LinkedList<>());
        extractVariables(astNode);
    }

    protected GraphNode(NodeType type, String instruction, @NotNull N astNode, List<VariableAction> variableActions) {
        this(IdHelper.getInstance().getNextId(), type, instruction, astNode, variableActions);
    }

    protected GraphNode(long id, NodeType type, String instruction, @NotNull N astNode, List<VariableAction> variableActions) {
        this.id = id;
        this.nodeType = type;
        this.instruction = instruction;
        this.astNode = astNode;
        this.variableActions = variableActions;
    }

    protected void extractVariables(@NotNull Node node) {
        new VariableVisitor(this::addUsedVariable, this::addDefinedVariable, this::addDeclaredVariable).search(node);
    }

    public long getId() {
        return id;
    }

    public String toString() {
        return String.format("GraphNode{id: %s, type: %s, instruction: '%s', astNodeType: %s}",
                getId(),
                getNodeType(),
                getInstruction(),
                getAstNode().getClass().getSimpleName()
        );
    }

    public N getAstNode() {
        return astNode;
    }

    public void addDeclaredVariable(NameExpr variable) {
        variableActions.add(new VariableAction.Declaration(variable, this));
    }

    public VariableAction.Definition addDefinedVariable(NameExpr variable) {
        VariableAction.Definition def = new VariableAction.Definition(variable, this);
        variableActions.add(def);
        return def;
    }

    public VariableAction.Usage addUsedVariable(NameExpr variable) {
        VariableAction.Usage use = new VariableAction.Usage(variable, this);
        variableActions.add(use);
        return use;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof GraphNode))
            return false;

        GraphNode<?> other = (GraphNode<?>) o;

        return Objects.equals(getId(), other.getId())
                && Objects.equals(getNodeType(), other.getNodeType())
                && Objects.equals(getInstruction(), other.getInstruction())
                && Objects.equals(astNode, other.astNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getNodeType(), getInstruction(), getAstNode());
    }

    public List<VariableAction> getVariableActions() {
        return Collections.unmodifiableList(variableActions);
    }

    public String getInstruction() {
        return instruction;
    }

    @Override
    public int compareTo(@NotNull GraphNode<?> o) {
        return Long.compare(id, o.id);
    }

    public NodeType getNodeType() {
        return nodeType;
    }
}
