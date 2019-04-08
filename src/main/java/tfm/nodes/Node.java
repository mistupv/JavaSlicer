package tfm.nodes;

import edg.graphlib.Vertex;
import tfm.arcs.data.ArcData;
import tfm.graphs.Graph;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Node extends Vertex<String, ArcData> {

    private int fileLineNumber;

//    public Node(Graph.NodeId id, String instruction) {
//        this(id, instruction, null);
//    }

    public Node(Graph.NodeId id, String instruction, Integer fileLineNumber) {
        super(id.toString(), instruction);

        this.fileLineNumber = fileLineNumber;
    }

    public int getId() {
        return Integer.parseInt(getName());
    }

    public String toString() {
        return String.format("Node{id: %s, data: '%s', in: %s, out: %s}",
                getName(),
                getData(),
                getIncomingArrows().stream().map(arrow -> arrow.getFrom().getName()).collect(Collectors.toList()),
                getOutgoingArrows().stream().map(arc -> arc.getTo().getName()).collect(Collectors.toList()));
    }

    public int getFileLineNumber() {
        return fileLineNumber;
    }

    public void setFileLineNumber(Integer fileLineNumber) {
        this.fileLineNumber = fileLineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Node))
            return false;

        Node other = (Node) o;

        return Objects.equals(getData(), other.getData())
                && Objects.equals(getIncomingArrows(), other.getIncomingArrows())
                && Objects.equals(getOutgoingArrows(), other.getOutgoingArrows())
                && Objects.equals(fileLineNumber, other.fileLineNumber);
                // && Objects.equals(getName(), other.getName()) ID IS ALWAYS UNIQUE, SO IT WILL NEVER BE THE SAME
    }

    public String toGraphvizRepresentation() {
        return String.format("%s[label=\"%s: %s\"];", getId(), getId(), getData());
    }
}
