package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.Optional;

/** A slicing criterion, or the point of interest in slicing. The selected variable(s)
 *  (if any) must produce the same sequence of values in the original program as in the sliced one. */
public abstract class SlicingCriterion {
    protected final String variable;

    public SlicingCriterion(String variable) {
        this.variable = variable;
    }

    public String getVariable() {
        return variable;
    }

    /** Locate the slicing criterion in a control flow graph. */
    public abstract Optional<GraphNode<?>> findNode(CFG graph);
    /** Locate the slicing criterion in a program dependence graph. */
    public abstract Optional<GraphNode<?>> findNode(PDG graph);
    /** Locate the slicing criterion in a system dependence graph. */
    public abstract Optional<GraphNode<?>> findNode(SDG graph);

    @Override
    public String toString() {
        return "(" + variable + ")";
    }
}
