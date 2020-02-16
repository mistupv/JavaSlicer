package tfm.slicing;

import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
import tfm.nodes.GraphNode;

import java.util.Optional;

public abstract class SlicingCriterion {

    protected String variable;

    public SlicingCriterion(String variable) {
        this.variable = variable;
    }

    public String getVariable() {
        return variable;
    }

    public abstract Optional<GraphNode<?>> findNode(CFG graph);
    public abstract Optional<GraphNode<?>> findNode(PDG graph);
    public abstract Optional<GraphNode<?>> findNode(SDG graph);

    @Override
    public String toString() {
        return "(" + variable + ")";
    }
}
