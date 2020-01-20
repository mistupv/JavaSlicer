package tfm.arcs;

import org.jgrapht.graph.DefaultEdge;
import tfm.nodes.GraphNode;

import java.util.Objects;
import java.util.Optional;

public abstract class Arc extends DefaultEdge {

    private String variable;

    public Arc() {

    }

    public Arc(String variable) {
        this.variable = variable;
    }

    public abstract boolean isControlFlowArrow();

    public abstract boolean isControlDependencyArrow();

    public abstract boolean isDataDependencyArrow();

    public Optional<String> getVariable() {
        return Optional.ofNullable(this.variable);
    }

    @Override
    public String toString() {
        return toGraphvizRepresentation();
    }

    public String toGraphvizRepresentation() {
        GraphNode<?> from = (GraphNode<?>) getSource();
        GraphNode<?> to = (GraphNode<?>) getTarget();

        return String.format("%s -> %s",
                from.getId(),
                to.getId()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        return Objects.equals(variable, ((Arc) o).variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variable, getSource(), getTarget());
    }
}
