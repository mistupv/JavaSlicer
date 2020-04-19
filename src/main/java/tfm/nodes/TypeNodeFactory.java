package tfm.nodes;

import com.github.javaparser.ast.Node;
import org.jetbrains.annotations.NotNull;
import tfm.nodes.type.NodeType;

import java.util.Collection;
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
            @NotNull Collection<String> declaredVariables,
            @NotNull Collection<String> definedVariables,
            @NotNull Collection<String> usedVariables
    ) {
        Objects.requireNonNull(instruction, "Instruction cannot be null!");
        Objects.requireNonNull(node, "AST Node cannot be null");
        Objects.requireNonNull(declaredVariables, "declared variables collection cannot be null!");
        Objects.requireNonNull(definedVariables, "defined variables collection cannot be null");
        Objects.requireNonNull(usedVariables, "Used variables collection cannot be null!");

        return new GraphNode<>(
                IdHelper.getInstance().getNextId(),
                getSpecificType(),
                instruction,
                node,
                declaredVariables,
                definedVariables,
                usedVariables
        );
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
