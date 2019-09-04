package tfm.arcs;

import tfm.arcs.data.ArcData;
import tfm.nodes.GraphNode;

import java.util.Objects;

public abstract class Arc<D extends ArcData> extends edg.graphlib.Arrow<String, D> {

    @SuppressWarnings("unchecked")
    public Arc(GraphNode<?> from, GraphNode<?> to) {
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
        GraphNode from = (GraphNode) getFrom();
        GraphNode to = (GraphNode) getTo();

        return String.format("%s -> %s",
                from.getId(),
                to.getId()
        );
    }

    public GraphNode<?> getFromNode() {
        return (GraphNode<?>) super.getFrom();
    }

    public GraphNode<?> getToNode() {
        return (GraphNode<?>) super.getTo();
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

        GraphNode from = (GraphNode) arc.getFrom();
        GraphNode from2 = (GraphNode) getFrom();
        GraphNode to = (GraphNode) getTo();
        GraphNode to2 = (GraphNode) arc.getTo();

        return Objects.equals(arc.getData(), getData()) &&
                Objects.equals(from.getId(), from2.getId()) &&
                Objects.equals(to.getId(), to2.getId());
    }
}
