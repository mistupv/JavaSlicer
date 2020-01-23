package tfm.graphs;

import tfm.slicing.Slice;
import tfm.slicing.SlicingCriterion;

public interface Sliceable {
    Slice slice(SlicingCriterion sc);
}
