package tfm.slicing;

import tfm.nodes.GraphNode;

public interface SlicingAlgorithm {
    Slice traverse(GraphNode<?> slicingCriterion);
}
