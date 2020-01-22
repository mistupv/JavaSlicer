package tfm.graphs;

import com.github.javaparser.ast.Node;
import tfm.nodes.GraphNode;
import tfm.nodes.NodeFactory;

import java.util.Objects;
import java.util.Optional;

public abstract class GraphWithRootNode<ASTRootNode extends Node> extends Graph {

    protected final int ROOT_NODE_ID = 0;

    protected GraphNode<ASTRootNode> rootNode;

    public GraphWithRootNode() {
        super(1);
    }

    /**
     * Builds the root node with the given instruction and AST node.
     * If the root node already exists, just returns false
     *
     * @param instruction the instruction string
     * @param rootNodeAst the AST node
     * @return true if the root node is created, false otherwise
     */
    public boolean buildRootNode(String instruction, ASTRootNode rootNodeAst) {
        if (rootNode != null) {
            return false;
        }

        GraphNode<ASTRootNode> root = NodeFactory.graphNode(ROOT_NODE_ID, instruction, rootNodeAst);
        this.rootNode = root;
        this.addVertex(root);

        return true;
    }

    public Optional<GraphNode<?>> getRootNode() {
        return Optional.ofNullable(rootNode);
    }

    @Override
    public boolean removeVertex(GraphNode<?> graphNode) {
        if (Objects.equals(graphNode, rootNode)) {
            return false;
        }

        return super.removeVertex(graphNode);
    }
}
