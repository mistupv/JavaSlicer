package tfm.graphs;

import com.github.javaparser.ast.Node;
import edg.graphlib.Arrow;
import edg.graphlib.Vertex;
import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A graphlib Graph without cost and data in arcs
 * */
public abstract class Graph extends edg.graphlib.Graph<String, ArcData> {

    private int nextVertexId = 0;

//    public final static class NodeId {
//        private static int nextVertexId = 0;
//
//        private int id;
//
//        private NodeId(int id) {
//            this.id = id;
//        }
//
//        static synchronized NodeId getVertexId() {
//            return new NodeId(nextVertexId++);
//        }
//
//        public int getId() {
//            return id;
//        }
//
//        @Override
//        public String toString() {
//            return String.valueOf(id);
//        }
//    }

    public Graph() {
        super();
    }

    /**
      Library fix: if a node had an edge to itself resulted in 2 outgoing nodes instead of 1 outgoing and 1 incoming
     */
    @Override
    public boolean addEdge(Arrow<String, ArcData> arrow) {
        Vertex<String, ArcData> from = arrow.getFrom();
        Vertex<String, ArcData> to = arrow.getTo();
        int cost = arrow.getCost();
        ArcData data = arrow.getData();

        if (!verticies.contains(from))
            throw new IllegalArgumentException(String.format("from (%s) is not in graph", from));
        if (!verticies.contains(to))
            throw new IllegalArgumentException(String.format("to (%s) is not in graph", to));

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
    public <ASTNode extends Node> GraphNode<ASTNode> getRootNode() {
        return (GraphNode<ASTNode>) super.getRootVertex();
    }

    public abstract <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node);

    @SuppressWarnings("unchecked")
    public <ASTNode extends Node> Optional<GraphNode<ASTNode>> findNodeByASTNode(ASTNode astNode) {
        return getNodes().stream()
                .filter(node -> Objects.equals(node.getAstNode(), astNode))
                .findFirst()
                .map(node -> (GraphNode<ASTNode>) node);
    }

    public Optional<GraphNode<?>> findNodeById(int id) {
        return getNodes().stream()
                .filter(node -> Objects.equals(node.getId(), id))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    public Set<GraphNode<?>> getNodes() {
        return getVerticies().stream()
                .map(vertex -> (GraphNode<?>) vertex)
                .collect(Collectors.toSet());
    }

    public Set<Arc<ArcData>> getArcs() {
        return getArrows().stream()
                .map(arrow -> (Arc<ArcData>) arrow)
                .collect(Collectors.toSet());
    }

    public String toString() {
        return getNodes().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .map(GraphNode::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public abstract String toGraphvizRepresentation();

    protected synchronized int getNextVertexId() {
        return nextVertexId++;
    }

    public boolean contains(GraphNode<?> graphNode) {
        return getNodes().stream()
                .anyMatch(node -> Objects.equals(node, graphNode));
    }

    /**
     * Deprecated for incorrect behaviour. Use removeNode instead
     */
    @Override
    @Deprecated
    public boolean removeVertex(Vertex<String, ArcData> vertex) {
        throw new UnsupportedOperationException("Deprecated method. Use removeNode instead");
    }

    public void removeNode(GraphNode<?> node) {
        verticies.remove(node);

        edges.removeAll(node.getOutgoingArcs());
        edges.removeAll(node.getIncomingArcs());
    }

    public List<GraphNode<?>> findDeclarationsOfVariable(String variable) {
        return getNodes().stream()
                .filter(node -> node.getDeclaredVariables().contains(variable))
                .collect(Collectors.toList());
    }
}
