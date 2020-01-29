package tfm.graphs.pdg;

import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.function.Supplier;

public class DirectedTree<V, E> extends DirectedAcyclicGraph<V, E> {
    protected V root;

    public DirectedTree(Supplier<V> vertexSupplier, Supplier<E> edgeSupplier, boolean weighted) {
        super(vertexSupplier, edgeSupplier, weighted);
    }

    public V getRoot() {
        return root;
    }

    @Override
    public boolean addEdge(V sourceVertex, V targetVertex, E defaultEdge) {
        if (inDegreeOf(targetVertex) >= 1)
            throw new IllegalArgumentException("The target vertex already has a parent. This should be a tree!");
        return super.addEdge(sourceVertex, targetVertex, defaultEdge);
    }
}
