package tfm.graphs.cfg;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.GraphWithRootNode;
import tfm.nodes.GraphNode;
import tfm.utils.NodeNotFoundException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The <b>Control Flow Graph</b> represents the statements of a method in
 * a graph, displaying the connections between each statement and the ones that
 * may follow it. You can build one manually or use the {@link CFGBuilder CFGBuilder}.
 * The variations of the CFG are implemented as child classes.
 * @see ControlFlowArc
 */
public class CFG extends GraphWithRootNode<MethodDeclaration> {
    private boolean built = false;
    protected GraphNode<?> exitNode;

    public CFG() {
        super();
    }

    public void addControlFlowEdge(GraphNode<?> from, GraphNode<?> to) {
        addControlFlowEdge(from, to, new ControlFlowArc());
    }

    protected void addControlFlowEdge(GraphNode<?> from, GraphNode<?> to, ControlFlowArc arc) {
        super.addEdge(from, to, arc);
    }

    public Set<GraphNode<?>> findLastDefinitionsFrom(GraphNode<?> startNode, String variable) {
        if (!this.containsVertex(startNode))
            throw new NodeNotFoundException(startNode, this);
        return findLastDefinitionsFrom(new HashSet<>(), startNode, startNode, variable);
    }

    private Set<GraphNode<?>> findLastDefinitionsFrom(Set<Integer> visited, GraphNode<?> startNode, GraphNode<?> currentNode, String variable) {
        visited.add(currentNode.getId());

        Set<GraphNode<?>> res = new HashSet<>();

        for (Arc arc : incomingEdgesOf(currentNode)) {
            if (!arc.isExecutableControlFlowArc())
                continue;
            GraphNode<?> from = getEdgeSource(arc);

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

    @Override
    public boolean removeVertex(GraphNode<?> graphNode) {
        if (Objects.equals(graphNode, exitNode))
            return false;
        return super.removeVertex(graphNode);
    }

    public GraphNode<?> getExitNode() {
        return exitNode;
    }

    protected void setExitNode(GraphNode<?> exitNode) {
        if (this.exitNode != null)
            throw new IllegalStateException("Exit node already set!");
        this.exitNode = exitNode;
    }

    @Override
    public void build(MethodDeclaration method) {
        method.accept(newCFGBuilder(), null);
        if (exitNode == null)
            throw new IllegalStateException("Exit node missing!");
        built = true;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    protected CFGBuilder newCFGBuilder() {
        return new CFGBuilder(this);
    }
}
