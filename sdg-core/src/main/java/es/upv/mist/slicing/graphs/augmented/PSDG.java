package es.upv.mist.slicing.graphs.augmented;

import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.slicing.PseudoPredicateSlicingAlgorithm;
import es.upv.mist.slicing.slicing.SlicingAlgorithm;

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
