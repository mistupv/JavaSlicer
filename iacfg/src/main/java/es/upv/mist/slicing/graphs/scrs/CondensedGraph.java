package es.upv.mist.slicing.graphs.scrs;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.Map;
import java.util.function.Supplier;

/**
 * An abstract class for condensed graphs.
 * @param <V> The type of the original graph's vertices.
 * @param <E> The type of the original graph's edges.
 * @param <R> The type of this graph's strongly connected regions.
 * @see AbstractSCRAlgorithm
 */
public abstract class CondensedGraph<V, E, R extends AbstractSCR<V, E>> extends SimpleDirectedGraph<R, DefaultEdge> {
    protected Map<V, R> regionMap;

    public CondensedGraph(Class<? extends DefaultEdge> edgeClass) {
        super(edgeClass);
    }

    public CondensedGraph(Supplier<R> vertexSupplier, Supplier<DefaultEdge> edgeSupplier, boolean weighted) {
        super(vertexSupplier, edgeSupplier, weighted);
    }

    /**
     * Obtains the strongly connected region in which a given vertex is located.
     * Only return {@code null} when the given vertex did not belong to the original graph.
     */
    public R getRegion(V v) {
        return regionMap.get(v);
    }
}
