package es.upv.mist.slicing.graphs.scrs;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper for a Strong Connectivity Algorithm, to modify the generated condensation and enable the modification
 * of such condensations, by using {@link AbstractSCR}.<br>
 * A condensation of the strongly connected regions of a {@code Graph<A, B>} with vertices of type {@code A}
 * and edges of type {@code B} is {@code Graph<Graph<A, B>, DefaultEdge>}, a new graph whose vertices are subgraphs
 * (strongly connected regions) of the initial graph, connected by {@link DefaultEdge}.<br>
 * This class uses a more specific type for the vertices of the condensation, yielding {@code Graph<R, DefaultEdge>}
 * for the condensation. This type is a wrapper on the subgraph, with implementations of {@code hashCode} and {@code equals}
 * that do not change when the graph changes, therefore allowing modifications.
 * @param <V> The type of vertices on the original graph.
 * @param <E> The type of edges on the original graph.
 * @param <R> The type of vertices on the condensed graph.
 * @see org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm
 * @see AbstractSCR
 */
public abstract class AbstractSCRAlgorithm<V, E, R extends AbstractSCR<V, E>> extends KosarajuStrongConnectivityInspector<V, E> {
    public AbstractSCRAlgorithm(Graph<V, E> graph) {
        super(graph);
    }

    public abstract R newSCR(Graph<V, E> graph, Set<V> nodeSet, Set<E> edgeSet);

    /**
     * Reimplementation of {@link org.jgrapht.alg.connectivity.AbstractStrongConnectivityInspector#getCondensation()} to wrap each strongly
     * connected component into a class with constant {@code hashCode} and {@code equals} implementation.
     *
     * @return A map, connecting each vertex in the original map to its location (region) in the condensation.
     */
    public Map<V, R> copySCRs(Graph<R, DefaultEdge> condensation) {
        List<Set<V>> sets = stronglyConnectedSets();

        Map<V, R> vertexToComponent = new HashMap<>();

        for (Set<V> set : sets) {
            R component = newSCR(graph, set, null);
            condensation.addVertex(component);
            for (V v : set) {
                vertexToComponent.put(v, component);
            }
        }

        for (E e : graph.edgeSet()) {
            V s = graph.getEdgeSource(e);
            R sComponent = vertexToComponent.get(s);

            V t = graph.getEdgeTarget(e);
            R tComponent = vertexToComponent.get(t);

            if (sComponent != tComponent) { // reference equal on purpose
                // TODO: instead of DefaultEdges, use a grouping class to hold multiple data edges (Arc, CallGraph.Edge)
                condensation.addEdge(sComponent, tComponent);
            }
        }

        return vertexToComponent;
    }
}
