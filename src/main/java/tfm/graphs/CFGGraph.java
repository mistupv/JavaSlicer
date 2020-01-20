package tfm.graphs;

import com.github.javaparser.ast.stmt.EmptyStmt;
import org.jgrapht.io.DOTExporter;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.nodes.GraphNode;
import tfm.utils.NodeNotFoundException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CFGGraph extends GraphWithRootNode {

    public CFGGraph() {
        super();
    }

    @Override
    protected GraphNode<?> buildRootNode() {
        return new GraphNode<>(getNextVertexId(), "Start", new EmptyStmt());
    }

    public void addControlFlowEdge(GraphNode<?> from, GraphNode<?> to) {
        super.addEdge(from, to, new ControlFlowArc());
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
            ControlFlowArc controlFlowArc = arc.asControlFlowArc();

            GraphNode<?> from = this.getEdgeSource(controlFlowArc);

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
}
