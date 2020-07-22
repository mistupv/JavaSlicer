package tfm.nodes;

import com.github.javaparser.ast.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface NodeFactory {

    /**
     * Returns a computed GraphNode (i.e. a GraphNode with computed the
     * declared, defined and used variables in its AST node)
     *
     * @param instruction the instruction that represents
     * @param node the node of the AST that represents
     * @param variableActions the list of variable actions performed in this node
     * @param <ASTNode> the type of the AST node
     * @return a new GraphNode
     */
    <ASTNode extends Node> GraphNode<ASTNode> computedGraphNode(
            @NotNull String instruction,
            @NotNull ASTNode node,
            @NotNull List<VariableAction> variableActions
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
