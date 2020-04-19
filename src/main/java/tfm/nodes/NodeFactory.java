package tfm.nodes;

import com.github.javaparser.ast.Node;
import org.jetbrains.annotations.NotNull;
import tfm.nodes.GraphNode;

import java.util.Collection;

public interface NodeFactory {

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
    <ASTNode extends Node> GraphNode<ASTNode> computedGraphNode(
            @NotNull String instruction,
            @NotNull ASTNode node,
            @NotNull Collection<String> declaredVariables,
            @NotNull Collection<String> definedVariables,
            @NotNull Collection<String> usedVariables
    );


    /**
     * Returns a GraphNode computing the declared, defined and used variables in its AST node
     *
     * @param instruction the instruction that represents
     * @param node the node of the AST that represents
     * @param <ASTNode> the type of the AST node
     * @return a new GraphNode
     */
    public <ASTNode extends Node> GraphNode<ASTNode> graphNode(
            @NotNull String instruction,
            @NotNull ASTNode node
    );
}
