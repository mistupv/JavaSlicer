package tfm.nodes;

import com.github.javaparser.ast.Node;
import org.jetbrains.annotations.NotNull;
import tfm.nodes.type.NodeType;

import java.util.List;
import java.util.Objects;

public abstract class TypeNodeFactory implements NodeFactory {

    public static TypeNodeFactory fromType(NodeType type) {
        return new TypeNodeFactory() {
            @Override
            protected NodeType getSpecificType() {
                return type;
            }
        };
    }

    public <ASTNode extends Node> GraphNode<ASTNode> computedGraphNode(
            @NotNull String instruction,
            @NotNull ASTNode node,
            @NotNull List<VariableAction> variableActions
    ) {
        Objects.requireNonNull(instruction, "Instruction cannot be null!");
        Objects.requireNonNull(node, "AST Node cannot be null");
        Objects.requireNonNull(variableActions, "declared variables collection cannot be null!");

        return new GraphNode<>(IdHelper.getInstance().getNextId(), getSpecificType(), instruction, node, variableActions);
    }

    public <ASTNode extends Node> GraphNode<ASTNode> graphNode(
            @NotNull String instruction,
            @NotNull ASTNode node
    ) {
        Objects.requireNonNull(instruction, "Instruction cannot be null!");
        Objects.requireNonNull(node, "AST Node cannot be null");

        return new GraphNode<>(IdHelper.getInstance().getNextId(), getSpecificType(), instruction, node);
    }

    protected abstract NodeType getSpecificType();
}
