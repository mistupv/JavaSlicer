package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.EmptyStmt;
import edg.graphlib.Arrow;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.NodeNotFoundException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The <b>Control Flow Graph</b> represents the statements of a method in
 * a graph, displaying the connections between each statement and the ones that
 * may follow it. You can build one manually or use the {@link tfm.visitors.cfg.CFGBuilder CFGBuilder}.
 * The variations of the CFG are implemented as child classes.
 * @see ControlFlowArc
 */
public class CFG extends Graph {

    public CFG() {
        super();
        setRootVertex(new GraphNode<>(getNextVertexId(), getRootNodeData(), new EmptyStmt()));
    }

    @Override
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        GraphNode<ASTNode> vertex = new GraphNode<>(getNextVertexId(), instruction, node);
        this.addVertex(vertex);

        return vertex;
    }


    protected String getRootNodeData() {
        return "Start";
    }

    public void addControlFlowEdge(GraphNode<?> from, GraphNode<?> to) {
        addControlFlowEdge(from, to, true);
    }

    @SuppressWarnings("unchecked")
    protected void addControlFlowEdge(GraphNode<?> from, GraphNode<?> to, boolean executable) {
        if (executable)
            super.addEdge((Arrow) new ControlFlowArc(from, to));
        else
            super.addEdge((Arrow) new ControlFlowArc.NonExecutable(from, to));
    }

    @Override
    public String toGraphvizRepresentation() {
        String lineSep = System.lineSeparator();

        String nodes = getNodes().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .map(GraphNode::toGraphvizRepresentation)
                .collect(Collectors.joining(lineSep));

        String arrows =
                getArrows().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((GraphNode<?>) arrow.getFrom()).getId()))
                        .map(arrow -> ((Arc<?>) arrow).toGraphvizRepresentation())
                        .collect(Collectors.joining(lineSep));

        return "digraph g{" + lineSep +
                nodes + lineSep +
                arrows + lineSep +
                "}";
    }

    @Override
    public Set<Integer> slice(SlicingCriterion slicingCriterion) {
        throw new RuntimeException("Can't slice a CFG!");
    }

    public Set<GraphNode<?>> findLastDefinitionsFrom(GraphNode<?> startNode, String variable) {
        if (!this.contains(startNode)) {
            throw new NodeNotFoundException(startNode, this);
        }
        
        return findLastDefinitionsFrom(new HashSet<>(), startNode, startNode, variable);
    }

    private Set<GraphNode<?>> findLastDefinitionsFrom(Set<Integer> visited, GraphNode<?> startNode, GraphNode<?> currentNode, String variable) {
        visited.add(currentNode.getId());
        Set<GraphNode<?>> res = new HashSet<>();

        for (Arc<?> arc : currentNode.getIncomingArcs()) {
            ControlFlowArc controlFlowArc = (ControlFlowArc) arc;
            // Ignore non-executable edges when computing data dependence.
            if (arc instanceof ControlFlowArc.NonExecutable)
                continue;

            GraphNode<?> from = controlFlowArc.getFromNode();

            if (!Objects.equals(startNode, from) && visited.contains(from.getId())) {
                continue;
            }

            if (from.getDefinedVariables().contains(variable)) {
                res.add(from);
            } else {
                res.addAll(findLastDefinitionsFrom(visited, startNode, from, variable));
            }
        }

        return res;
    }

    public static class ACFG extends CFG {
        public void addNonExecutableControlFlowEdge(GraphNode<?> from, GraphNode<?> to) {
            addControlFlowEdge(from, to, false);
        }
    }
}
