package tfm.graphs;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.nodes.GraphNode;
import tfm.utils.NodeNotFoundException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CFG extends GraphWithRootNode<MethodDeclaration> {

    public CFG() {
        super();
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
