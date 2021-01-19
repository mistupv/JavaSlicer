package es.upv.mist.slicing.graphs;

import es.upv.mist.slicing.utils.ASTUtils;
import org.jgrapht.graph.AbstractGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A fixed-point analysis algorithm that propagates changes backwards through a given graph.
 * @param <V> The graph's vertices type.
 * @param <E> The graph's edges type.
 * @param <D> The value to be computed for each vertex.
 *           It should have a valid {@link Object#equals(Object)} implementation.
 */
public abstract class BackwardDataFlowAnalysis<V, E, D> {
    /** The graph on which this algorithm iterates. */
    protected final AbstractGraph<V, E> graph;
    /** A mapping of the latest value computed per node. */
    protected final Map<V, D> vertexDataMap = new HashMap<>();

    protected boolean built = false;

    public BackwardDataFlowAnalysis(AbstractGraph<V, E> graph) {
        this.graph = graph;
    }

    /** Iterate through the graph until a fixed-point is reached.
     *  This method only performs the analysis on its first call. */
    public void analyze() {
        assert !built;
        List<V> workList = new LinkedList<>(graph.vertexSet());
        graph.vertexSet().forEach(v -> vertexDataMap.put(v, initialValue(v)));
        while (!workList.isEmpty()) {
            List<V> newWorkList = new LinkedList<>();
            for (V vertex : workList) {
                Set<V> mayAffectVertex = graph.outgoingEdgesOf(vertex).stream()
                        .map(graph::getEdgeTarget).collect(Collectors.toCollection(ASTUtils::newIdentityHashSet));
                D newValue = compute(vertex, mayAffectVertex);
                if (!Objects.equals(vertexDataMap.get(vertex), newValue)) {
                    vertexDataMap.put(vertex, newValue);
                    graph.incomingEdgesOf(vertex).stream().map(graph::getEdgeSource).forEach(newWorkList::add);
                }
            }
            workList = newWorkList;
        }
        built = true;
    }

    /** Compute a new value for a given vertex, given a set of nodes that might affect its value. */
    protected abstract D compute(V vertex, Set<V> predecessors);

    /** Compute the initial value for a given vertex. */
    protected abstract D initialValue(V vertex);
}
