package tfm.graphs;

import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import edg.graphlib.Vertex;
import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.nodes.Node;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A graphlibGraph without cost and data in arcs
 * */
public abstract class Graph<NodeType extends Node> extends edg.graphlib.Graph<String, ArcData> {

    public final static class NodeId {
        private static int nextVertexId = 0;

        private int id;

        private NodeId(int id) {
            this.id = id;
        }

        static synchronized NodeId getVertexId() {
            return new NodeId(nextVertexId++);
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

    public Graph() {
        super();
    }

    @Override
    /*
      Library fix: if a node had an edge to itself resulted in 2 outgoing nodes instead of 1 outgoing and 1 incoming
     */
    public boolean addEdge(Arrow<String, ArcData> arrow) {
        Vertex<String, ArcData> from = arrow.getFrom();
        Vertex<String, ArcData> to = arrow.getTo();
        int cost = arrow.getCost();
        ArcData data = arrow.getData();

        if (!verticies.contains(from))
            throw new IllegalArgumentException("from is not in graph");
        if (!verticies.contains(to))
            throw new IllegalArgumentException("to is not in graph");

        List<Arrow<String, ArcData>> es2 = from.findEdges(to);

        for (Arrow<String, ArcData> e2 : es2) {
            if (e2 != null && cost == e2.getCost() &&
                    ((data == null && e2.getData() == null) ||
                            (data != null && data.equals(e2.getData()))))
                return false;
        }

        // FIX
        if (Objects.equals(from, to)) {
            from.getOutgoingArrows().add(arrow);
            from.getIncomingArrows().add(arrow);
        } else {
            from.addEdge(arrow);
            to.addEdge(arrow);
        }
        edges.add(arrow);
        return true;
    }

    @SuppressWarnings("unchecked")
    public NodeType getRootNode() {
        return (NodeType) super.getRootVertex();
    }

    public abstract NodeType addNode(String instruction, Statement statement);

    public String toString() {
        return getVerticies().stream()
                .map(edg.graphlib.Vertex::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public abstract String toGraphvizRepresentation();

}
