package tfm.graphs;

import tfm.slicing.SlicingCriterion;

public interface Sliceable<G> {
    G slice(SlicingCriterion sc);
}
