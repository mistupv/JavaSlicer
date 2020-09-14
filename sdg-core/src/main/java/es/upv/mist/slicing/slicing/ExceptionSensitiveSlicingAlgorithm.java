package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.arcs.sdg.InterproceduralArc;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.utils.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An exception-sensitive slicing algorithm, which follows these rules:
 * <ol>
 *     <li>SDG: two-pass traversal, first ignoring interprocedural outputs, then interprocedural inputs.</li>
 *     <li>PPDG*: pseudo-predicate nodes that are included only due to control-dependency arcs should
 *          not keep traversing control-dependency arcs.</li>
 *     <li>CCD: a node reached by a CC1 or CC2 arc is not included in the slice until it has either been
 *          reached by an unconditional dependency or by the opposite (CC2 or CC1, respectively) arc.</li>
 *     <li>CCD: a node included only due to conditional-control-dependency should not keep traversing any arc.</li>
 *     <li>CCD (apply only if none of the previous allow for a new node and this does): CC1 arcs are
 *          transitively traversed, even when the intermediate nodes are not (yet) included in the slice.</li>
 *          TODO: last rule hasn't been completely included, the parenthesis is not implemented.
 * </ol>
 */
public class ExceptionSensitiveSlicingAlgorithm implements SlicingAlgorithm {
    /** Return values for handlers. A node can either be skipped, traversed or not handled.
     * In the second case, the handler is responsible of modifying the state of the algorithm.
     * In the last case, the following handler is called. */
    protected static final int SKIPPED = 0, TRAVERSED = 1, NOT_HANDLED = 2;

    protected final ESSDG graph;
    protected GraphNode<?> slicingCriterion;

    /** Nodes that already in the slice and whose arcs have all been traversed. */
    protected final Set<GraphNode<?>> visited = new HashSet<>();
    /** Nodes already in the slice whose arcs have been partially visited.
     * The value stored is the number of arcs remaining */
    protected final Map<GraphNode<?>, Integer> partlyVisited = new HashMap<>();
    /** Arcs that have already been traversed. No arc must be traversed twice. */
    protected final Set<Arc> traversedArcs = new HashSet<>();
    /** Nodes that have been reached via an unconditional arc.
     * The next step is to traverse their arcs and move them to 'visited' */
    protected final Set<GraphNode<?>> reached = new HashSet<>();
    /** Current SDG check (it changes depending on the pass). */
    protected Predicate<Arc> sdgSkipCheck;

    /** Arc handlers, in the order they are executed. Each handler is a predicate,
     * if the arc is handled and added or skipped it should return true, for the
     * chain to be stopped. Otherwise, it should return false and the next handler
     * will try to handle it. The last handler must always handle the arcs that
     * reach it (i.e. return true). */
    protected final List<Function<Arc, Integer>> HANDLERS = List.of(
            this::handleRepeats,
            arc -> sdgSkipCheck.test(arc) ? SKIPPED : NOT_HANDLED,
            this::ppdgSkipCheck,
            this::handleExceptionSensitive,
            this::handleDefault
    );

    public ExceptionSensitiveSlicingAlgorithm(ESSDG graph) {
        this.graph = Objects.requireNonNull(graph);
    }

    @Override
    public Slice traverseProcedure(GraphNode<?> slicingCriterion) {
        this.slicingCriterion = slicingCriterion;
        reached.add(slicingCriterion);
        sdgSkipCheck = InterproceduralArc.class::isInstance;
        pass();
        return createSlice();
    }

    @Override
    public Slice traverse(GraphNode<?> slicingCriterion) {
        this.slicingCriterion = slicingCriterion;
        reached.add(slicingCriterion);
        sdgSkipCheck = Arc::isInterproceduralOutputArc;
        pass();
        reached.addAll(partlyVisited.keySet());
        sdgSkipCheck = Arc::isInterproceduralInputArc;
        pass();
        return createSlice();
    }

    /** Generate a slice object from the sets of visited and partly visited nodes. */
    protected Slice createSlice() {
        Slice slice = new Slice();
        // Removes nodes that have only been visited by one kind of conditional control dependence
        Predicate<GraphNode<?>> pred = n -> slicingCriterion.equals(n) ||
                (!hasOnlyBeenReachedBy(n, ConditionalControlDependencyArc.CC1.class) && !hasOnlyBeenReachedBy(n, ConditionalControlDependencyArc.CC2.class));
        visited.stream().filter(pred).forEach(slice::add);
        partlyVisited.keySet().stream().filter(pred).forEach(slice::add);
        return slice;
    }

    /** Perform a round of traversal, until no new nodes can be added. */
    protected void pass() {
        while (!reached.isEmpty()) {
            GraphNode<?> node = Utils.setPop(reached);
            // Avoid duplicate traversal
            if (visited.contains(node))
                continue;
            // Traverse all edges backwards
            Set<Arc> incoming = graph.incomingEdgesOf(node);
            int remaining = partlyVisited.getOrDefault(node, incoming.size());
            arcLoop: for (Arc arc : incoming) {
                for (Function<Arc, Integer> handler : HANDLERS)
                    switch (handler.apply(arc)) {
                        case TRAVERSED:   // the arc has been traversed, count down and stop applying handlers
                            remaining--;
                        case SKIPPED:     // stop applying handlers, go to next arc
                            continue arcLoop;
                        case NOT_HANDLED: // try the next handler
                            break;
                        default:
                            throw new UnsupportedOperationException("Handler answer not considered in switch");
                    }
                throw new IllegalStateException("This arc has not been handled by any handler");
            }
            if (remaining == 0) {
                visited.add(node);
                partlyVisited.remove(node);
            } else {
                partlyVisited.put(node, remaining);
            }
        }
    }

    /** The default handler, which traverses unconditionally the arc. */
    protected int handleDefault(Arc arc) {
        GraphNode<?> src = graph.getEdgeSource(arc);
        traversedArcs.add(arc);
        if (!visited.contains(src))
            reached.add(src);
        return TRAVERSED;
    }

    /** A handler that discards any arc that has already been processed. */
    protected int handleRepeats(Arc arc) {
        return traversedArcs.contains(arc) ? SKIPPED : NOT_HANDLED;
    }

    /** A handler that skips control dependency arcs that shall not be traversed due to the PPDG* rule. */
    protected int ppdgSkipCheck(Arc arc) {
        GraphNode<?> node = graph.getEdgeTarget(arc);
        return !node.equals(slicingCriterion)
                && graph.isPseudoPredicate(node)
                && hasOnlyBeenReachedBy(node, ControlDependencyArc.class)
                && arc.isUnconditionalControlDependencyArc()
                ? SKIPPED : NOT_HANDLED;
    }

    /** A handler that skips arcs when a node has only been reached by CCD. */
    protected int handleExceptionSensitive(Arc arc) {
        GraphNode<?> node = graph.getEdgeTarget(arc);
        // Visit only CC1 if only CC1 has visited it
        if (hasOnlyBeenReachedBy(node, ConditionalControlDependencyArc.CC1.class) && arc instanceof ConditionalControlDependencyArc.CC1)
            return NOT_HANDLED;
        // Visit none if the node has only been reached by conditional arcs
        if (!node.equals(slicingCriterion) && reachedStream(node).allMatch(Arc::isConditionalControlDependencyArc))
            return SKIPPED;
        // Otherwise (has been visited by other arcs) continue as normal
        return NOT_HANDLED;
    }

    /** Check if a node hasn't been reached or it only has been reached by arcs of a given class. */
    protected boolean hasOnlyBeenReachedBy(GraphNode<?> node, Class<? extends Arc> type) {
        return reachedStream(node).allMatch(a -> a.getClass().equals(type));
    }

    /** Obtain a stream of arcs that have reached the given node. */
    protected Stream<Arc> reachedStream(GraphNode<?> node) {
        return traversedArcs.stream().filter(arc -> graph.getEdgeSource(arc).equals(node));
    }
}
