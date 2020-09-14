package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.Optional;

/** A criterion that locates nodes by {@link GraphNode}. */
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
