package tfm.slicing;

import tfm.arcs.Arc;
import tfm.graphs.Graph;
import tfm.nodes.GraphNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ClassicSlicingAlgorithm implements SlicingAlgorithm {
    protected final Graph graph;

    public ClassicSlicingAlgorithm(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Slice traverse(GraphNode<?> slicingCriterion) {
        Slice slice = new Slice();
        slice.add(slicingCriterion);
        pass(slice, this::ignorePass1);
        pass(slice, this::ignorePass2);
        return slice;
    }

    protected boolean ignorePass1(Arc arc) {
        return arc.isInterproceduralOutputArc();
    }

    protected boolean ignorePass2(Arc arc) {
        return arc.isInterproceduralInputArc();
    }

    protected void pass(Slice slice, Predicate<Arc> ignoreCondition) {
        // `toVisit` behaves like a set and using iterable we can use it as a queue
        // More info: https://stackoverflow.com/a/2319126
        LinkedHashSet<GraphNode<?>> toVisit = new LinkedHashSet<>(slice.getGraphNodes());
        Set<GraphNode<?>> visited = new HashSet<>();

        while (!toVisit.isEmpty()) {
            GraphNode<?> node = removeFirst(toVisit);
            // Avoid duplicate traversal
            if (visited.contains(node))
                continue;
            visited.add(node);
            // Traverse all edges backwards
            for (Arc arc : graph.incomingEdgesOf(node)) {
                if (ignoreCondition.test(arc))
                    continue;
                GraphNode<?> source = graph.getEdgeSource(arc);
                if (!visited.contains(source))
                    toVisit.add(source);
            }
        }

        visited.forEach(slice::add);
    }

    protected static <E> E removeFirst(Set<E> set) {
        Iterator<E> i = set.iterator();
        E e = i.next();
        i.remove();
        return e;
    }
}
