package tfm.nodes;

import com.sun.corba.se.spi.ior.ObjectKey;
import tfm.arcs.data.ArcData;
import tfm.graphs.Graph;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Vertex extends edg.graphlib.Vertex<String, ArcData> {

    private Integer fileLineNumber;

    public Vertex(Graph.VertexId id, String instruction) {
        this(id, instruction, null);
    }

    public Vertex(Graph.VertexId id, String instruction, Integer fileLineNumber) {
        super(id.toString(), instruction);

        this.fileLineNumber = fileLineNumber;
    }

    public int getId() {
        return Integer.parseInt(getName());
    }

    public String toString() {
        return String.format("Vertex{id: %s, data: '%s', in: %s, out: %s}",
                getName(),
                getData(),
                getIncomingArrows().stream().map(arrow -> arrow.getFrom().getName()).collect(Collectors.toList()),
                getOutgoingArrows().stream().map(arc -> arc.getTo().getName()).collect(Collectors.toList()));
    }

    public Optional<Integer> getFileLineNumber() {
        return Optional.ofNullable(fileLineNumber);
    }

    public void setFileLineNumber(Integer fileLineNumber) {
        this.fileLineNumber = fileLineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Vertex))
            return false;

        Vertex other = (Vertex) o;

        return Objects.equals(getData(), other.getData())
                && Objects.equals(getIncomingArrows(), other.getIncomingArrows())
                && Objects.equals(getOutgoingArrows(), other.getOutgoingArrows())
                && Objects.equals(fileLineNumber, other.fileLineNumber);
                // && Objects.equals(getName(), other.getName()) ID IS ALWAYS UNIQUE, SO IT WILL NEVER BE THE SAME
    }
}
