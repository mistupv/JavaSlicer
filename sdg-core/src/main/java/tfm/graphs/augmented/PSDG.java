package tfm.graphs.augmented;

import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.nodes.GraphNode;
import tfm.nodes.SyntheticNode;
import tfm.slicing.PseudoPredicateSlicingAlgorithm;
import tfm.slicing.SlicingAlgorithm;

/** A pseudo-predicate SDG, equivalent to an ASDG that is built using the {@link PPDG} instead of {@link APDG}.
 * It uses a different slicing algorithm than its parent graphs. */
public class PSDG extends ASDG {
    @Override
    protected Builder createBuilder() {
        return new Builder();
    }

    @Override
    protected SlicingAlgorithm createSlicingAlgorithm() {
        return new PseudoPredicateSlicingAlgorithm(this);
    }

    /** @see ACFG#isPseudoPredicate(GraphNode) */
    public boolean isPseudoPredicate(GraphNode<?> node) {
        if (node instanceof SyntheticNode)
            return false;
        for (CFG cfg : cfgMap.values())
            if (cfg.containsVertex(node))
                return ((ACFG) cfg).isPseudoPredicate(node);
        throw new IllegalArgumentException("Node " + node.getId() + "'s associated CFG cannot be found!");
    }

    /** Populates a PSDG, using {@link ACFG} and {@link PPDG} as default graphs.
     * @see ASDG.Builder */
    public class Builder extends ASDG.Builder {
        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof ACFG;
            return new PPDG((ACFG) cfg);
        }
    }
}
