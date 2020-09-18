package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc.CC1;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc.CC2;
import es.upv.mist.slicing.arcs.sdg.InterproceduralArc;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.utils.Utils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An exception-sensitive slicing algorithm, which follows these rules:
 * <ol>
 *     <li>SDG: two-pass traversal, (a) first ignoring interprocedural outputs, (b) then interprocedural inputs.</li>
 *     <li>PPDG*: pseudo-predicate nodes that are included only due to control-dependency arcs should
 *          not keep traversing control-dependency arcs.</li>
 *     <li>CCD: a node reached by a CC1 or CC2 arc is not included in the slice until it has either been
 *          reached by an unconditional dependency or by the opposite (CC2 or CC1, respectively) arc.</li>
 *     <li>CCD: a node included only due to conditional-control-dependency should not keep traversing any arc,
 *          except for CD arcs that are exclusive to the PPDG.</li>
 *     <li>CCD (apply only if none of the previous allow for a new node and this does): CC1 arcs are
 *          transitively traversed, even when the intermediate nodes are not (yet) included in the slice.</li>
 * </ol>
 */
public class ExceptionSensitiveSlicingAlgorithm implements SlicingAlgorithm {
    protected static final Predicate<Arc> INTRAPROCEDURAL = InterproceduralArc.class::isInstance;
    /** Applies rule 1a of the algorithm. */
    protected static final Predicate<Arc> SDG_PASS_1 = Arc::isInterproceduralOutputArc;
    /** Applies rule 1b of the algorithm. */
    protected static final Predicate<Arc> SDG_PASS_2 = Arc::isInterproceduralInputArc;

    protected final ESSDG graph;
    protected GraphNode<?> slicingCriterion;

    /** Set of the arcs that have been traversed in the slicing process. */
    protected final Set<Arc> traversedArcSet = new HashSet<>();
    /** Similar to {@link #traversedArcSet} */
    protected final Map<GraphNode<?>, Set<Arc>> traversedArcMap = new HashMap<>();

    public ExceptionSensitiveSlicingAlgorithm(ESSDG graph) {
        this.graph = Objects.requireNonNull(graph);
    }

    @Override
    public Slice traverse(GraphNode<?> slicingCriterion) {
        this.slicingCriterion = slicingCriterion;
        Slice slice = new Slice();
        slice.add(slicingCriterion);
        pass(slice, SDG_PASS_1.or(this::ppdgIgnore).or(this::essdgIgnore));
        pass(slice, SDG_PASS_2.or(this::ppdgIgnore).or(this::essdgIgnore));
        return slice;
    }

    @Override
    public Slice traverseProcedure(GraphNode<?> slicingCriterion) {
        this.slicingCriterion = slicingCriterion;
        Slice slice = new Slice();
        slice.add(slicingCriterion);
        pass(slice, INTRAPROCEDURAL);
        return slice;
    }

    /**
     * Perform a round of traversal, until no new nodes can be added to the slice. Then, apply rule 5.
     * @param slice A slice object that will serve as initial work-list and where nodes will be added.
     * @param ignoreCondition A predicate used to ignore arcs, when they test true.
     */
    protected void pass(Slice slice, Predicate<Arc> ignoreCondition) {
        pass(slice, slice.getGraphNodes(), ignoreCondition);
    }

    /**
     * Perform a round of traversal, until no new nodes can be added to the slice. Then, apply rule 5.
     * @param slice A slice object where nodes will be added.
     * @param workList The initial work-list.
     * @param ignoreCondition A predicate used to ignore arcs, when they test true.
     */
    protected void pass(Slice slice, Set<GraphNode<?>> workList, Predicate<Arc> ignoreCondition) {
        Set<GraphNode<?>> pending = new HashSet<>(workList);
        Set<Arc> cc1s = new HashSet<>();
        while (!pending.isEmpty()) {
            GraphNode<?> node = Utils.setPop(pending);
            // Populate the map for this node (if empty)
            traversedArcMap.computeIfAbsent(node, n -> new HashSet<>());
            for (Arc arc : graph.incomingEdgesOf(node)) {
                if (arc instanceof CC1)
                    cc1s.add(arc);
                // Only traverse the arc if (1) it hasn't been traversed, (2) it hasn't been ignored
                if (!traversedArcMap.get(node).contains(arc) && !ignoreCondition.test(arc))
                    if (traverseArc(arc, slice))
                        pending.add(graph.getEdgeSource(arc));
            }
        }
        // Consider transitivity when there are no more arcs to traverse.
        cc1s.removeAll(traversedArcSet);
        while (!cc1s.isEmpty()) {
            Arc arc = Utils.setPop(cc1s);
            // If the target of the arc has been reached, but only by CC1, traverse the arc
            if (hasOnlyBeenReachedBy(graph.getEdgeTarget(arc), CC1.class)) {
                traverseArc(arc, slice);
                // Find the transitive CC1 edges and add them to the work-list
                for (Arc a : graph.incomingEdgesOf(graph.getEdgeSource(arc)))
                    if (a instanceof CC1)
                        cc1s.add(a);
            }
        }
    }

    /** Applies rule 2 of the algorithm. */
    protected boolean ppdgIgnore(Arc arc) {
        GraphNode<?> target = graph.getEdgeTarget(arc);
        return arc.isUnconditionalControlDependencyArc() &&
                reachedStream(target).allMatch(Arc::isUnconditionalControlDependencyArc) &&
                !target.equals(slicingCriterion);
    }

    /** Applies rule 4 of the algorithm. */
    protected boolean essdgIgnore(Arc arc) {
        GraphNode<?> target = graph.getEdgeTarget(arc);
        if (arc.isUnconditionalControlDependencyArc() && arc.asControlDependencyArc().isPPDGExclusive())
            return false;
        return hasOnlyBeenReachedBy(target, ConditionalControlDependencyArc.class);
    }

    /**
     * Registers the arc as traversed, and if allowed by rule 3 of the algorithm, includes the source
     * of the arc in the slice.
     * @return If the source node should be added to the work-list.
     */
    protected boolean traverseArc(Arc arc, Slice slice) {
        traversedArcMap.get(graph.getEdgeTarget(arc)).add(arc);
        traversedArcSet.add(arc);
        GraphNode<?> source = graph.getEdgeSource(arc);
        if (!hasOnlyBeenReachedBy(source, CC1.class) && !hasOnlyBeenReachedBy(source, CC2.class)) {
            if (!slice.contains(source))
                slice.add(source);
            int sourceArcsTraversed = traversedArcMap.getOrDefault(source, Collections.emptySet()).size();
            return  sourceArcsTraversed != graph.incomingEdgesOf(source).size();
        }
        return false;
    }

    /** Check if a node only has been reached by arcs of a given class. */
    protected boolean hasOnlyBeenReachedBy(GraphNode<?> node, Class<? extends Arc> type) {
        return reachedStream(node).count() > 0 && reachedStream(node).allMatch(type::isInstance);
    }

    /** Obtain a stream of arcs that have reached the given node. */
    protected Stream<Arc> reachedStream(GraphNode<?> node) {
        return traversedArcSet.stream().filter(arc -> graph.getEdgeSource(arc).equals(node));
    }
}
