package tfm.slicing;

import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
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
