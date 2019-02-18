package tfm.jacosro.graphs;

import tfm.jacosro.nodes.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Graph<T, E extends Node<T>> {

    private static int nextNodeId = 0;

    protected List<E> nodes;

    public Graph() {
        nodes = new ArrayList<>();
    }

    public E getRoot() {
        return nodes.get(0);
    }

    public abstract E addNode(T data);

    public String toString() {
        return nodes.stream()
                .map(Node::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    protected synchronized int getNextNodeId() {
        return nextNodeId++;
    }
}
