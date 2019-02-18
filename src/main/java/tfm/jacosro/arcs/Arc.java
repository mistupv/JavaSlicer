package tfm.jacosro.arcs;

import org.jetbrains.annotations.NotNull;
import tfm.jacosro.nodes.Node;

import java.util.Objects;

public abstract class Arc<E extends Node> implements Comparable<Arc<E>> {

    protected E source;
    protected E target;

    protected Arc(E source, E target) {
        this.source = source;
        this.target = target;
    }

    public E getSource() {
        return source;
    }

    public E getTarget() {
        return target;
    }

    public boolean isInArcOf(E node) {
        return Objects.equals(node, target);
    }

    public boolean isOutArcOf(E node) {
        return Objects.equals(node, source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Arc))
            return false;

        Arc other = (Arc) o;

        return source.equals(other.source)
                && target.equals(other.target);
    }

    @Override
    public int compareTo(@NotNull Arc o) {
        return Integer.compare(source.getId(), o.source.getId());
    }
}
