package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.Optional;

/** A criterion that locates nodes by numerical id. */
public class NodeIdSlicingCriterion extends SlicingCriterion {
    protected final long id;

    public NodeIdSlicingCriterion(long id, String variable) {
        super(variable);
        this.id = id;
    }

    @Override
    public Optional<GraphNode<?>> findNode(CFG graph) {
        return graph.findNodeById(id);
    }

    @Override
    public Optional<GraphNode<?>> findNode(PDG graph) {
        return graph.findNodeById(id);
    }

    @Override
    public Optional<GraphNode<?>> findNode(SDG graph) {
        return graph.findNodeById(id);
    }
}
