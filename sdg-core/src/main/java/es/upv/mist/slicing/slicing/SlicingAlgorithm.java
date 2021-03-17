package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.nodes.GraphNode;

import java.util.Set;

public interface SlicingAlgorithm {
    /** Obtain the nodes reached by this algorithm in a classic 2-pass interprocedural slice. */
    Slice traverse(Set<GraphNode<?>> slicingCriterion);
    /** Obtain the nodes reached by this algorithm intraprocedurally (i.e. without traversing interprocedural arcs. */
    Slice traverseProcedure(GraphNode<?> slicingCriterion);
}
