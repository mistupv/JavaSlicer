package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.jetbrains.annotations.NotNull;
import tfm.nodes.GraphNode;
import tfm.nodes.factories.NodeFactory;

import java.util.Objects;
import java.util.Optional;

public abstract class GraphWithRootNode<ASTRootNode extends Node> extends Graph implements Buildable<MethodDeclaration> {

    protected GraphNode<ASTRootNode> rootNode;

    protected GraphWithRootNode() {
        super();
    }

    /**
     * Builds the root node with the given instruction and AST node.
     * If the root node already exists, just returns false
     *
     * @param instruction the instruction string
     * @param rootNodeAst the AST node
     * @return true if the root node is created, false otherwise
     */
    public boolean buildRootNode(@NotNull String instruction, @NotNull ASTRootNode rootNodeAst, @NotNull NodeFactory nodeFactory) {
        if (rootNode != null) {
            return false;
        }

        GraphNode<ASTRootNode> root = nodeFactory.graphNode(instruction, rootNodeAst);
        this.rootNode = root;
        this.addVertex(root);

        return true;
    }

    public Optional<GraphNode<ASTRootNode>> getRootNode() {
        return Optional.ofNullable(rootNode);
    }

    public void setRootNode(GraphNode<ASTRootNode> rootNode) {
        if (!this.containsVertex(rootNode)) {
            throw new IllegalArgumentException("Cannot set root node: " + rootNode + " is not contained in graph!");
        }

        this.rootNode = rootNode;
    }

    @Override
    public boolean removeVertex(GraphNode<?> graphNode) {
        if (Objects.equals(graphNode, rootNode)) {
            return false;
        }

        return super.removeVertex(graphNode);
    }
}
