package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.NoSuchElementException;
import java.util.Set;

/** A slicing criterion, or the point of interest in slicing. The selected variable(s)
 *  (if any) must produce the same sequence of values in the original program as in the sliced one. */
public interface SlicingCriterion {
    /**
     * Locates the nodes that represent the slicing criterion in the SDG.
     * @return A set with at least one element, all of which directly represent
     *         the selected criterion in the SDG.
     * @throws NoSuchElementException When the slicing criterion cannot be located.
     */
    Set<GraphNode<?>> findNode(SDG sdg);
}
