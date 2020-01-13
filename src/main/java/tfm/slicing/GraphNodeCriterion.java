package tfm.slicing;

import tfm.graphs.CFG;
import tfm.graphs.PDG;
import tfm.graphs.SDG;
import tfm.nodes.GraphNode;

import java.util.Optional;

public class GraphNodeCriterion extends SlicingCriterion {
    private final GraphNode<?> node;

    public GraphNodeCriterion(GraphNode<?> node, String variable) {
        super(variable);
        this.node = node;
    }

    @Override
    public Optional<GraphNode<?>> findNode(CFG graph) {
        return graph.findNodeById(node.getId());
    }

    @Override
    public Optional<GraphNode<?>> findNode(PDG graph) {
        return graph.findNodeById(node.getId());
    }

    @Override
    public Optional<GraphNode<?>> findNode(SDG graph) {
        return graph.findNodeById(node.getId());
    }
}
