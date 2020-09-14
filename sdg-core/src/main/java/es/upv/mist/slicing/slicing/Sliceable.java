package es.upv.mist.slicing.slicing;

public interface Sliceable {
    /** Extract a subset of nodes that affect the given slicing criterion. */
    Slice slice(SlicingCriterion sc);
}
