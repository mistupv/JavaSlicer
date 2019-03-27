package tfm.nodes;

import tfm.arcs.data.ArcData;
import tfm.graphs.Graph;

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
}
