package tfm.utils;

import com.github.javaparser.ast.Node;
import tfm.nodes.GraphNode;

import java.util.Collection;

public class NodeFactory {

    /**
     * Returns a computed GraphNode (i.e. a GraphNode with computed the
     * declared, defined and used variables in its AST node)
     *
     * @param id the id of the node
     * @param instruction the instruction that represents
     * @param node the node of the AST that represents
     * @param declaredVariables the set of declared variables
     * @param definedVariables the set of defined variables
     * @param usedVariables the set of used variables
     * @param <ASTNode> the type of the AST node
     * @return a new GraphNode
     */
    public static <ASTNode extends Node> GraphNode<ASTNode> computedGraphNode(
            int id,
            String instruction,
            ASTNode node,
            Collection<String> declaredVariables,
            Collection<String> definedVariables,
            Collection<String> usedVariables
    ) {
        return new GraphNode<>(
                id,
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
     * @param id the id of the node
     * @param instruction the instruction that represents
     * @param node the node of the AST that represents
     * @param <ASTNode> the type of the AST node
     * @return a new GraphNode
     */
    public static <ASTNode extends Node> GraphNode<ASTNode> graphNode(
            int id,
            String instruction,
            ASTNode node
    ) {
        return new GraphNode<>(
                id,
                instruction,
                node
        );
    }
}
