package tfm.graphs;

import com.github.javaparser.ast.Node;
import org.jgrapht.graph.DefaultDirectedGraph;
import tfm.arcs.Arc;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * */
public abstract class Graph extends DefaultDirectedGraph<GraphNode<?>, Arc> {

    private int nextVertexId = 0;

    public Graph() {
        super(null, null, false);
    }

    private <ASTNode extends Node> GraphNode<ASTNode> addNode(GraphNode<ASTNode> node) {
        this.addVertex(node);

        return node;
    }

    private <ASTNode extends Node> GraphNode<ASTNode> addNode(int id, String instruction, ASTNode node) {
        GraphNode<ASTNode> newNode = new GraphNode<>(id, instruction, node);

        return this.addNode(newNode);
    }

    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        return this.addNode(getNextVertexId(), instruction, node);
    }

    /**
     * Adds the given node to the graph.
     *
     * One must be careful with this method, as the given node will have
     * an id and arcs corresponding to the graph in which it was created, and may not fit
     * in the current graph.
     *
     * @param node the node to add to the graph
     * @param copyId whether to copy the id node or not
     * @param copyArcs whether to copy the arcs of the node or not
     * @return the node instance added to the graph
     */
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(GraphNode<ASTNode> node, boolean copyId, boolean copyArcs) {
        GraphNode<ASTNode> res = node;

        if (copyId && copyArcs) {
            this.addVertex(node);
        } else if (copyId) {
            res = this.addNode(node.getId(), node.getInstruction(), node.getAstNode());
        } else if (copyArcs) {
            res = new GraphNode<>(node);
            res.setId(getNextVertexId());

            this.addVertex(res);
        } else {
            res = this.addNode(node.getInstruction(), node.getAstNode());
        }

        return res;
    }

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

    public Set<GraphNode<?>> getNodes() {
        return vertexSet();
    }

    public Set<Arc> getArcs() {
        return edgeSet();
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
        return super.containsVertex(graphNode);
    }

    public abstract Graph slice(SlicingCriterion slicingCriterion);

    public void removeNode(GraphNode<?> node) {
        removeVertex(node);
    }

    public List<GraphNode<?>> findDeclarationsOfVariable(String variable) {
        return getNodes().stream()
                .filter(node -> node.getDeclaredVariables().contains(variable))
                .collect(Collectors.toList());
    }
}
