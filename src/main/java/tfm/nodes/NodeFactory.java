package tfm.nodes;

import com.github.javaparser.ast.Node;

import java.util.Collection;
import java.util.Objects;

public class NodeFactory {

    /**
     * Returns a computed GraphNode (i.e. a GraphNode with computed the
     * declared, defined and used variables in its AST node)
     *
     * @param instruction the instruction that represents
     * @param node the node of the AST that represents
     * @param declaredVariables the set of declared variables
     * @param definedVariables the set of defined variables
     * @param usedVariables the set of used variables
     * @param <ASTNode> the type of the AST node
     * @return a new GraphNode
     */
    public static <ASTNode extends Node> GraphNode<ASTNode> computedGraphNode(
            String instruction,
            ASTNode node,
            Collection<String> declaredVariables,
            Collection<String> definedVariables,
            Collection<String> usedVariables
    ) {
        Objects.requireNonNull(instruction, "Instruction cannot be null!");
        Objects.requireNonNull(node, "AST Node cannot be null");
        Objects.requireNonNull(declaredVariables, "declared variables collection cannot be null!");
        Objects.requireNonNull(definedVariables, "defined variables collection cannot be null");
        Objects.requireNonNull(usedVariables, "Used variables collection cannot be null!");

        return new GraphNode<>(
                IdHelper.getInstance().getNextId(),
                instruction,
                node,
                declaredVariables,
                definedVariables,
                usedVariables
        );
    }

    /**
     * Returns a GraphNode computing the declared, defined and used variables in its AST node
     *
     * @param instruction the instruction that represents
     * @param node the node of the AST that represents
     * @param <ASTNode> the type of the AST node
     * @return a new GraphNode
     */
    public static <ASTNode extends Node> GraphNode<ASTNode> graphNode(
            String instruction,
            ASTNode node
    ) {
        Objects.requireNonNull(instruction, "Instruction cannot be null!");
        Objects.requireNonNull(node, "AST Node cannot be null");

        return new GraphNode<>(
                IdHelper.getInstance().getNextId(),
                instruction,
                node
        );
    }
}
