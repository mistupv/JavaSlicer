package tfm.arcs;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.Attribute;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.GraphNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class Arc extends DefaultEdge {
    public Arc() {

    }

    public final boolean isControlFlowArc() {
        return this instanceof ControlFlowArc;
    }

    public final ControlFlowArc asControlFlowArc() {
        if (isControlFlowArc())
            return (ControlFlowArc) this;
        throw new UnsupportedOperationException("Not a ControlFlowArc");
    }

    public final boolean isControlDependencyArc() {
        return this instanceof ControlDependencyArc;
    }

    public final ControlDependencyArc asControlDependencyArc() {
        if (isControlDependencyArc())
            return (ControlDependencyArc) this;
        throw new UnsupportedOperationException("Not a ControlDependencyArc");
    }

    public final boolean isDataDependencyArc() {
        return this instanceof DataDependencyArc;
    }

    public final DataDependencyArc asDataDependencyArc() {
        if (isDataDependencyArc())
            return (DataDependencyArc) this;
        throw new UnsupportedOperationException("Not a DataDependencyArc");
    }

    @Override
    public String toString() {
        return String.format("%s{%d -> %d}", getClass().getName(),
                ((GraphNode<?>) getSource()).getId(), ((GraphNode<?>) getTarget()).getId());
    }

    public String getLabel() {
        return "";
    }

    public Map<String, Attribute> getDotAttributes() {
        return new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!o.getClass().equals(this.getClass()))
            return false;
        return Objects.equals(getSource(), ((Arc) o).getSource()) &&
                Objects.equals(getTarget(), ((Arc) o).getTarget());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getSource(), getTarget());
    }
}
