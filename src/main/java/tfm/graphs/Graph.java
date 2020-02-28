package tfm.graphs;

import com.github.javaparser.ast.Node;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.io.DOTExporter;
import tfm.arcs.Arc;
import tfm.nodes.GraphNode;
import tfm.nodes.NodeFactory;
import tfm.utils.ASTUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * */
public abstract class Graph extends DirectedPseudograph<GraphNode<?>, Arc> {

    protected Graph() {
        super(null, null, false);
    }

    /**
     * Adds the given node to the graph.
     *
     * @param node the node to add to the graph
     */
    public <ASTNode extends Node> void addNode(GraphNode<ASTNode> node) {
        this.addVertex(node);
    }

    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        GraphNode<ASTNode> newNode = NodeFactory.graphNode(instruction, node);

        this.addNode(newNode);

        return newNode;
    }

    @SuppressWarnings("unchecked")
    public <ASTNode extends Node> Optional<GraphNode<ASTNode>> findNodeByASTNode(ASTNode astNode) {
        return vertexSet().stream()
                .filter(node -> ASTUtils.equalsWithRangeInCU(node.getAstNode(), astNode))
                .findFirst()
                .map(node -> (GraphNode<ASTNode>) node);
    }

    public Optional<GraphNode<?>> findNodeById(long id) {
        return vertexSet().stream()
                .filter(node -> Objects.equals(node.getId(), id))
                .findFirst();
    }

    @Override
    public String toString() {
        return vertexSet().stream().sorted()
                .map(GraphNode::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public boolean isEmpty() {
        return this.vertexSet().isEmpty();
    }

    public DOTExporter<GraphNode<?>, Arc> getDOTExporter() {
        return new DOTExporter<>(
                graphNode -> String.valueOf(graphNode.getId()),
                GraphNode::getInstruction,
                Arc::getLabel,
                null,
                Arc::getDotAttributes);
    }
}
