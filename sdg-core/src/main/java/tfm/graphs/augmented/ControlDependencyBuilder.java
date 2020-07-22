package tfm.graphs.augmented;

import tfm.arcs.Arc;
import tfm.nodes.GraphNode;

import java.util.Set;
import java.util.stream.Collectors;

public class ControlDependencyBuilder extends tfm.graphs.pdg.ControlDependencyBuilder {
    public ControlDependencyBuilder(ACFG cfg, PPDG pdg) {
        super(cfg, pdg);
    }

    protected boolean postdominates(GraphNode<?> a, GraphNode<?> b, Set<GraphNode<?>> visited) {
        // Stop w/ success if a == b or a has already been visited
        if (a.equals(b) || visited.contains(a))
            return true;
        Set<Arc> outgoing = cfg.outgoingEdgesOf(a);
        // Limit the traversal if it is a PPDG
        outgoing = outgoing.stream().filter(Arc::isExecutableControlFlowArc).collect(Collectors.toSet());
        // Stop w/ failure if there are no edges to traverse from a
        if (outgoing.isEmpty())
            return false;
        // Find all possible paths starting from a, if ALL find b, then true, else false
        visited.add(a);
        for (Arc out : outgoing) {
            if (!postdominates(cfg.getEdgeTarget(out), b, visited))
                return false;
        }
        return true;
    }
}
