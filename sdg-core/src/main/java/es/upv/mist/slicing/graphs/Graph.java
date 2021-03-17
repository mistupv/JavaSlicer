package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.Node;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Utils;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.dot.DOTExporter;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** A generic base graph with some utility methods to more easily create and locate nodes. */
public abstract class Graph extends DirectedPseudograph<GraphNode<?>, Arc> {
    protected Graph() {
        super(null, null, false);
    }

    /** Creates and adds to the graph a normal node with the given label and AST. */
    public <T extends Node> GraphNode<T> addVertex(String label, T astNode) {
        GraphNode<T> node = new GraphNode<>(label, astNode);
        addVertex(node);
        return node;
    }

    /** Search for a node in this graph, given its AST node. If multiple nodes exist, due to some
     *  of them being synthetic, the non-synthetic one will be returned. <br/>
     *  @throws IllegalStateException If there are multiple non-synthetic nodes in this graph
     *                                representing the given AST node. */
    @SuppressWarnings("unchecked")
    public <T extends Node> Optional<GraphNode<T>> findNodeByASTNode(T astNode) {
        Set<GraphNode<?>> set = findAllNodes(n -> ASTUtils.equalsWithRangeInCU(n.getAstNode(), astNode));
        if (set.isEmpty())
            return Optional.empty();
        if (set.size() == 1)
            return Optional.of((GraphNode<T>) set.iterator().next());
        set.removeIf(SyntheticNode.class::isInstance);
        if (set.isEmpty())
            return Optional.empty();
        if (set.size() == 1)
            return Optional.of((GraphNode<T>) set.iterator().next());
        throw new IllegalStateException("There may only be one real node representing each AST node in the graph!");
    }

    /** Search for a node in the graph given a predicate it must pass.
     *  If multiple nodes match the predicate, the first one found is returned. */
    public Optional<GraphNode<?>> findNodeBy(Predicate<GraphNode<?>> p) {
        return vertexSet().stream().filter(p).findFirst();
    }

    /** Search for all nodes that match the given predicate. */
    public Set<GraphNode<?>> findAllNodes(Predicate<GraphNode<?>> p) {
        return vertexSet().stream().filter(p).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return vertexSet().stream().sorted()
                .map(GraphNode::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /** Obtain an appropriate DOT exporter for graphs based on this class. */
    public DOTExporter<GraphNode<?>, Arc> getDOTExporter() {
        DOTExporter<GraphNode<?>, Arc> dot = new DOTExporter<>();
        dot.setVertexIdProvider(n -> String.valueOf(n.getId()));
        dot.setVertexAttributeProvider(n -> Utils.dotLabel(n.getLongLabel()));
        dot.setEdgeAttributeProvider(Arc::getDotAttributes);
        return dot;
    }
}
