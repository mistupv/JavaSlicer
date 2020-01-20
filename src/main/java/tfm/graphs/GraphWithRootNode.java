package tfm.graphs;

import tfm.nodes.GraphNode;

import java.util.Objects;

public abstract class GraphWithRootNode extends Graph {

    protected GraphNode<?> rootNode;

    public GraphWithRootNode() {
        super();

        this.rootNode = buildRootNode();
        this.addVertex(rootNode);
    }

    protected abstract GraphNode<?> buildRootNode();

    public GraphNode<?> getRootNode() {
        return rootNode;
    }

    @Override
    public boolean removeVertex(GraphNode<?> graphNode) {
        if (Objects.equals(graphNode, rootNode)) {
            return false;
        }

        return super.removeVertex(graphNode);
    }
}
