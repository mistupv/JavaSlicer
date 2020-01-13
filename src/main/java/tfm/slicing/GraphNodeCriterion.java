package tfm.slicing;

import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.GraphNode;

import java.util.Optional;

public class GraphNodeCriterion extends SlicingCriterion {
    private final GraphNode<?> node;

    public GraphNodeCriterion(GraphNode<?> node, String variable) {
        super(variable);
        this.node = node;
    }

    @Override
    public Optional<GraphNode<?>> findNode(CFGGraph graph) {
        return graph.findNodeById(node.getId());
    }

    @Override
    public Optional<GraphNode<?>> findNode(PDGGraph graph) {
        return graph.findNodeById(node.getId());
    }

    @Override
    public Optional<GraphNode<?>> findNode(SDGGraph graph) {
        return graph.findNodeById(node.getId());
    }
}
