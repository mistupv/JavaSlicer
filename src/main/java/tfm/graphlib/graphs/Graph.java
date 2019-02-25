package tfm.graphlib.graphs;

import tfm.graphlib.arcs.Arc;
import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.nodes.Vertex;

import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * A graphlibGraph without cost and data in arcs
 * */
public abstract class Graph<NodeType extends Vertex> extends edg.graphlib.Graph<String, ArcData> {

    public final static class VertexId {
        private static int nextVertexId = 0;

        private int id;

        private VertexId(int id) {
            this.id = id;
        }

        static synchronized VertexId getVertexId() {
            return new VertexId(nextVertexId++);
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

    @SuppressWarnings("unchecked")
    @Override
    public NodeType getRootVertex() {
        return (NodeType) super.getRootVertex();
    }

    public abstract NodeType addVertex(String instruction);

    public String toString() {
        return getVerticies().stream()
                .map(edg.graphlib.Vertex::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public String toGraphvizRepresentation() {
        String arrows =
                getArrows().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((Vertex) arrow.getFrom()).getId()))
                        .map(arrow -> ((Arc) arrow).toGraphvizRepresentation())
                        .collect(Collectors.joining(System.lineSeparator()));

        String lineSep = System.lineSeparator();

        return "digraph g{" + lineSep +
                    arrows + lineSep +
                "}";
    }

}
