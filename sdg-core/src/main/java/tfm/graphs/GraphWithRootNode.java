package tfm.graphs;

import com.github.javaparser.ast.body.CallableDeclaration;
import tfm.nodes.GraphNode;

import java.util.Objects;

public abstract class GraphWithRootNode<T extends CallableDeclaration<?>> extends Graph implements Buildable<CallableDeclaration<?>> {
    protected boolean built = false;
    protected GraphNode<T> rootNode;

    protected GraphWithRootNode() {
        super();
    }

    /** Builds and sets the root node with the given label and AST node.
     *  If the root node already exists, an error occurs. */
    public void buildRootNode(String label, T rootNodeAst) {
        if (rootNode != null)
            throw new IllegalStateException("This graph has already been built, a root node already exists.");
        setRootNode(addVertex(label, rootNodeAst));
    }

    /** The node marked as root of this graph. */
    public GraphNode<T> getRootNode() {
        if (rootNode == null)
            throw new IllegalStateException("Graph has no root node!");
        return rootNode;
    }

    /** Save a node within the graph as the root node. This allows the replacement of the root node. */
    public void setRootNode(GraphNode<T> rootNode) {
        addVertex(Objects.requireNonNull(rootNode));
        this.rootNode = rootNode;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    @Override
    public boolean removeVertex(GraphNode<?> graphNode) {
        if (Objects.equals(graphNode, rootNode))
            return false;
        return super.removeVertex(graphNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rootNode);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj)
                && obj instanceof GraphWithRootNode
                && Objects.equals(rootNode, ((GraphWithRootNode<?>) obj).rootNode);
    }
}
