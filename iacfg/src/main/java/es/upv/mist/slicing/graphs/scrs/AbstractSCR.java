package es.upv.mist.slicing.graphs.scrs;

import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;

import java.util.Objects;
import java.util.Set;

/**
 * An abstract Strongly Connected Region (or Component) of a graph g, implemented as a subgraph of g.
 * This SCR can be freely modified (edges, vertices added and removed), and it will remain a part of the condensation.
 * @param <V> The type of vertices in the original graph.
 * @param <E> The type of edges in the original graph.
 * @see AbstractSCRAlgorithm
 */
public abstract class AbstractSCR<V, E> extends AsSubgraph<V, E> {
    /** A unique id to */
    protected int id;

    public AbstractSCR(Graph<V, E> base, Set<? extends V> vertexSubset, Set<? extends E> edgeSubset) {
        super(base, vertexSubset, edgeSubset);
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AbstractSCR scr && this.id == scr.id;
    }
}
