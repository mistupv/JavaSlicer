package tfm.arcs;

import tfm.arcs.data.ArcData;
import tfm.nodes.Node;

import java.util.Objects;

public abstract class Arc<D extends ArcData> extends edg.graphlib.Arrow<String, D> {

    @SuppressWarnings("unchecked")
    public Arc(Node from, Node to) {
        super((edg.graphlib.Vertex<String, D>) from, (edg.graphlib.Vertex<String, D>) to);
    }

    public abstract boolean isControlFlowArrow();

    public abstract boolean isControlDependencyArrow();

    public abstract boolean isDataDependencyArrow();

    @Override
    public String toString() {
        return String.format("Arc{data: %s, %s -> %s}",
                getData(),
                getFrom(),
                getTo()
        );
    }

    public String toGraphvizRepresentation() {
        Node from = (Node) getFrom();
        Node to = (Node) getTo();

        return String.format("%s -> %s",
                from.getId(),
                to.getId()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getData()) + getFrom().hashCode() + getTo().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Arc))
            return false;

        Arc arc = (Arc) o;

        return Objects.equals(arc.getData(), getData()) &&
                Objects.equals(arc.getFrom(), getFrom()) &&
                Objects.equals(arc.getTo(), getTo());
    }
}
