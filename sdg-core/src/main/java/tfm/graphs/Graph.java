package tfm.graphs;

import com.github.javaparser.ast.Node;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.io.DOTExporter;
import tfm.arcs.Arc;
import tfm.nodes.GraphNode;
import tfm.nodes.NodeFactory;
import tfm.nodes.SyntheticNode;
import tfm.utils.ASTUtils;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        return this.addNode(instruction, node, GraphNode.DEFAULT_FACTORY);
    }

    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node, NodeFactory nodeFactory) {
        GraphNode<ASTNode> newNode = nodeFactory.graphNode(instruction, node);

        this.addNode(newNode);

        return newNode;
    }

    @SuppressWarnings("unchecked")
    public <ASTNode extends Node> Optional<GraphNode<ASTNode>> findNodeByASTNode(ASTNode astNode) {
        Set<GraphNode<?>> set = findAllNodes(n -> ASTUtils.equalsWithRangeInCU(n.getAstNode(), astNode));
        if (set.isEmpty())
            return Optional.empty();
        if (set.size() == 1)
            return Optional.of((GraphNode<ASTNode>) set.iterator().next());
        set.removeIf(SyntheticNode.class::isInstance);
        if (set.isEmpty())
            return Optional.empty();
        if (set.size() == 1)
            return Optional.of((GraphNode<ASTNode>) set.iterator().next());
        throw new IllegalStateException("There may only be one real node representing each AST node in the graph!");
    }

    public Optional<GraphNode<?>> findNodeById(long id) {
        return findNodeBy(n -> n.getId() == id);
    }

    public Optional<GraphNode<?>> findNodeBy(Predicate<GraphNode<?>> p) {
        return vertexSet().stream().filter(p).findFirst();
    }

    public Set<GraphNode<?>> findAllNodes(Predicate<GraphNode<?>> p) {
        return vertexSet().stream().filter(p).collect(Collectors.toSet());
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
                n -> n.getId() + ": " + n.getInstruction(),
                Arc::getLabel,
                null,
                Arc::getDotAttributes);
    }
}
