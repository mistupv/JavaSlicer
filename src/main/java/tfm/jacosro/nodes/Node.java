package tfm.jacosro.nodes;

import org.jetbrains.annotations.NotNull;
import tfm.jacosro.arcs.Arc;

import java.util.*;
import java.util.stream.Collectors;

public class Node<E> implements Comparable<Node<E>> {

    private int id;
    protected E data;
    protected List<Arc<? extends Node<E>>> inArcs;
    protected List<Arc<? extends Node<E>>> outArcs;

    public Node(int id, E data) {
        this.id = id;
        this.data = data;
        this.inArcs = new ArrayList<>();
        this.outArcs = new ArrayList<>();
    }

    protected void addInArcs(Collection<? extends Arc<? extends Node<E>>> arcs) {
        inArcs.addAll(arcs);
    }

    @SafeVarargs
    protected final <A extends Arc<? extends Node<E>>> void addInArcs(@org.jetbrains.annotations.NotNull A... arcs) {
        addInArcs(Arrays.asList(arcs));
    }


    protected void addOutArcs(Collection<? extends Arc<? extends Node<E>>> arcs) {
        outArcs.addAll(arcs);
    }

    @SafeVarargs
    protected final <A extends Arc<? extends Node<E>>> void addOutArcs(@org.jetbrains.annotations.NotNull A... arcs) {
        addOutArcs(Arrays.asList(arcs));
    }

    public int getId() {
        return id;
    }

    public Set<Arc<? extends Node<E>>> getInArcs() {
        return new HashSet<>(inArcs);
    }

    public Set<Arc<? extends Node<E>>> getOutArcs() {
        return new HashSet<>(outArcs);
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        return (o instanceof Node) && ((Node) o).id == id;
    }

    public E getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(@NotNull Node o) {
        return Integer.compare(id, o.id);
    }

    public String toString() {
        return String.format("Vertex{id: %s, data: '%s', fromNodes: %s, toNodes: %s}",
                id,
                data,
                inArcs.stream().map(arc -> arc.getSource().getId()).collect(Collectors.toList()),
                outArcs.stream().map(arc -> arc.getTarget().getId()).collect(Collectors.toList()));
    }
}
