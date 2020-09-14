package es.upv.mist.slicing.graphs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.cfg.ControlFlowArc;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple but slow finder of control dependencies.
 * <br/>
 * It has a polynomial complexity (between cubed and n**4) with respect to the number of nodes in the CFG.
 * It uses the following definition of control dependence:
 * <br/>
 * A node <i>b</i> is control dependent on another node <i>a</i> if and only if <i>b</i> post-dominates
 * one but not all of the successors of <i>a</i>.
 * <br/>
 * A node <i>b</i> post-dominates another node <i>a</i> if and only if <i>b</i> appears in every path
 * from <i>a</i> to the "Exit" node.
 * <br/>
 * There exist better, cheaper approaches that have linear complexity w.r.t. the number of edges in the CFG.
 * <b>Usage:</b> pass an empty {@link PDG} and a filled {@link CFG} and then run {@link #build()}.
 * This builder should only be used once, and then discarded.
 */
public class ControlDependencyBuilder {
    protected final CFG cfg;
    protected final PDG pdg;

    public ControlDependencyBuilder(CFG cfg, PDG pdg) {
        this.cfg = cfg;
        this.pdg = pdg;
    }

    /** Adds the {@link ControlDependencyArc CCD arcs}. This method should only be called
     * once per {@link PDG}, as multiple executions may create duplicate arcs. */
    public void build() {
        assert cfg.isBuilt();
        GraphNode<?> enterNode = cfg.getRootNode();
        GraphNode<?> exitNode = cfg.getExitNode();

        Arc enterExitArc = null;
        if (!cfg.containsEdge(enterNode, exitNode)) {
            enterExitArc = new ControlFlowArc();
            cfg.addEdge(enterNode, exitNode, enterExitArc);
        }

        Set<GraphNode<?>> nodes = pdg.vertexSet();
        for (GraphNode<?> a : nodes) {
            for (GraphNode<?> b : nodes) {
                if (a == b) continue;
                if (hasControlDependence(a, b))
                    pdg.addControlDependencyArc(a, b);
            }
        }

        if (enterExitArc != null)
            cfg.removeEdge(enterExitArc);
    }

    /** Whether the first argument has a control-flow effect on the execution of the second argument. */
    public boolean hasControlDependence(GraphNode<?> a, GraphNode<?> b) {
        int yes = 0;
        Set<Arc> arcs = cfg.outgoingEdgesOf(a);
        // Nodes with less than 1 outgoing arc cannot control another node.
        if (arcs.size() < 2)
            return false;
        for (Arc arc : arcs)
            if (postDominates(cfg.getEdgeTarget(arc), b))
                yes++;
        int no = arcs.size() - yes;
        return yes > 0 && no > 0;
    }

    /** Whether the second argument post-dominates the first, i.e. whether {@code b} appears in all
     *  paths from {@code a} to the Exit node. */
    public boolean postDominates(GraphNode<?> a, GraphNode<?> b) {
        return postDominates(a, b, new HashSet<>());
    }

    /** @see #postDominates(GraphNode, GraphNode) */
    protected boolean postDominates(GraphNode<?> a, GraphNode<?> b, Set<GraphNode<?>> visited) {
        // Stop w/ success if a == b or a has already been visited
        if (a.equals(b) || visited.contains(a))
            return true;
        Set<Arc> outgoing = cfg.outgoingEdgesOf(a);
        // Stop w/ failure if there are no edges to traverse from a
        if (outgoing.isEmpty())
            return false;
        // Find all possible paths starting from a, if ALL find b, then true, else false
        visited.add(a);
        for (Arc out : outgoing) {
            if (!postDominates(cfg.getEdgeTarget(out), b, visited))
                return false;
        }
        return true;
    }
}
