package tfm.nodes;

import com.github.javaparser.ast.Node;
import org.jetbrains.annotations.NotNull;
import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
import tfm.nodes.type.NodeType;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    private NodeType nodeType;

    private final long id;
    private final String instruction;
    private final N astNode;

    private final Set<String> declaredVariables;
    private final Set<String> definedVariables;
    private final Set<String> usedVariables;

    GraphNode(long id, NodeType type, String instruction, @NotNull N astNode) {
        this(
                id,
                type,
                instruction,
                astNode,
                Utils.emptySet(),
                Utils.emptySet(),
                Utils.emptySet()
        );

        extractVariables(astNode);
    }

    GraphNode(
                long id,
                NodeType type,
                String instruction,
                @NotNull N astNode,
                Collection<String> declaredVariables,
                Collection<String> definedVariables,
                Collection<String> usedVariables
    ) {
        this.id = id;
        this.nodeType = type;
        this.instruction = instruction;
        this.astNode = astNode;

        this.declaredVariables = new HashSet<>(declaredVariables);
        this.definedVariables = new HashSet<>(definedVariables);
        this.usedVariables = new HashSet<>(usedVariables);
    }

    private void extractVariables(@NotNull Node node) {
        new VariableExtractor()
                .setOnVariableDeclarationListener(this.declaredVariables::add)
                .setOnVariableDefinitionListener(this.definedVariables::add)
                .setOnVariableUseListener(this.usedVariables::add)
                .visit(node);
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

    public void addDeclaredVariable(String variable) {
        declaredVariables.add(variable);
    }

    public void addDefinedVariable(String variable) {
        definedVariables.add(variable);
    }

    public void addUsedVariable(String variable) {
        usedVariables.add(variable);
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

    public Set<String> getDeclaredVariables() {
        return declaredVariables;
    }

    public Set<String> getDefinedVariables() {
        return definedVariables;
    }

    public Set<String> getUsedVariables() {
        return usedVariables;
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
