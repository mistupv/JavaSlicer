package tfm.slicing;

import tfm.arcs.Arc;
import tfm.arcs.pdg.ConditionalControlDependencyArc.CC1;
import tfm.arcs.pdg.ConditionalControlDependencyArc.CC2;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.graphs.exceptionsensitive.ESSDG;
import tfm.nodes.GraphNode;
import tfm.utils.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

// It doesn't inherit from PPSlicingAlgorithm because it's more difficult that way,
// plus the PPDG is inherently wrong (see SAS2020 paper on exceptions).
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

    protected Slice createSlice() {
        Slice slice = new Slice();
        // Removes nodes that have only been visited by one kind of conditional control dependence
        Predicate<GraphNode<?>> pred = n -> slicingCriterion.equals(n) ||
                (!hasOnlyBeenReachedBy(n, CC1.class) && !hasOnlyBeenReachedBy(n, CC2.class));
        visited.stream().filter(pred).forEach(slice::add);
        partlyVisited.keySet().stream().filter(pred).forEach(slice::add);
        return slice;
    }

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

    protected int handleDefault(Arc arc) {
        GraphNode<?> src = graph.getEdgeSource(arc);
        traversedArcs.add(arc);
        if (!visited.contains(src))
            reached.add(src);
        return TRAVERSED;
    }

    protected int handleRepeats(Arc arc) {
        return traversedArcs.contains(arc) ? SKIPPED : NOT_HANDLED;
    }

    protected int ppdgSkipCheck(Arc arc) {
        GraphNode<?> node = graph.getEdgeTarget(arc);
        return !node.equals(slicingCriterion)
                && graph.isPseudoPredicate(node)
                && hasOnlyBeenReachedBy(node, ControlDependencyArc.class)
                && arc.isUnconditionalControlDependencyArc()
                ? SKIPPED : NOT_HANDLED;
    }

    protected int handleExceptionSensitive(Arc arc) {
        GraphNode<?> node = graph.getEdgeTarget(arc);
        // Visit only CC1 if only CC1 has visited it
        if (hasOnlyBeenReachedBy(node, CC1.class) && arc instanceof CC1)
            return NOT_HANDLED;
        // Visit none if the node has only been reached by conditional arcs
        if (!node.equals(slicingCriterion) && reachedStream(node).allMatch(Arc::isConditionalControlDependencyArc))
            return SKIPPED;
        // Otherwise (has been visited by other arcs) continue as normal
        return NOT_HANDLED;
    }

    protected boolean hasOnlyBeenReachedBy(GraphNode<?> node, Class<? extends Arc> type) {
        return reachedStream(node).allMatch(a -> a.getClass().equals(type));
    }

    protected Stream<Arc> reachedStream(GraphNode<?> node) {
        return traversedArcs.stream().filter(arc -> graph.getEdgeSource(arc).equals(node));
    }
}
