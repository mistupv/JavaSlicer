package tfm.slicing;

import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
import tfm.nodes.GraphNode;

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
