package tfm.visitors.pdg;

import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.graphs.CFG;
import tfm.graphs.PDG;
import tfm.graphs.PDG.PPDG;
import tfm.nodes.GraphNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A simple but slow finder of control dependencies.
 * <br/>
 * It has a polynomial complexity (between cubed and n**4) with respect to the number of nodes in the CFG.
 * It uses the following definition of control dependence:
 * <br/>
 * A node <i>b</i> is control dependent on another node <i>a</i> if and only if <i>b</i> postdominates
 * one but not all of the successors of <i>a</i>.
 * <br/>
 * A node <i>b</i> postdominates another node <i>a</i> if and only if <i>b</i> appears in every path
 * from <i>a</i> to the "Exit" node.
 * <br/>
 * There exist better, cheaper approaches that have linear complexity w.r.t. the number of edges in the CFG.
 * <b>Usage:</b> pass an empty {@link PDG} and a filled {@link CFG} and then run {@link #analyze()}.
 * This builder should only be used once, and then discarded.
 */
public class ControlDependencyBuilder {
    private final PDG pdg;
    private final CFG cfg;

    public ControlDependencyBuilder(PDG pdg, CFG cfg) {
        this.pdg = pdg;
        this.cfg = cfg;
    }

    public void analyze() {
        Map<GraphNode<?>, GraphNode<?>> nodeMap = new HashMap<>();
        nodeMap.put(cfg.getRootNode(), pdg.getRootNode());
        Set<GraphNode<?>> roots = new HashSet<>(cfg.getNodes());
        roots.remove(cfg.getRootNode());
        Set<GraphNode<?>> cfgNodes = new HashSet<>(cfg.getNodes());
        cfgNodes.removeIf(node -> node.getData().equals("Exit"));

        for (GraphNode<?> node : cfgNodes)
            registerNode(node, nodeMap);

        for (GraphNode<?> src : cfgNodes) {
            for (GraphNode<?> dest : cfgNodes) {
                if (src == dest) continue;
                if (hasControlDependence(src, dest)) {
                    pdg.addControlDependencyArc(nodeMap.get(src), nodeMap.get(dest));
                    if (pdg.contains(src))
                        roots.remove(dest);
                }
            }
        }
        // In the original definition, nodes were dependent by default on the Enter/Start node
        for (GraphNode<?> node : roots)
            if (!node.getData().equals("Exit"))
                pdg.addControlDependencyArc(pdg.getRootNode(), nodeMap.get(node));
    }

    public void registerNode(GraphNode<?> node, Map<GraphNode<?>, GraphNode<?>> nodeMap) {
        if (nodeMap.containsKey(node) || node.getData().equals("Exit"))
            return;
        GraphNode<?> clone = new GraphNode<>(node.getId(), node.getData(), node.getAstNode());
        nodeMap.put(node, clone);
        pdg.addVertex(clone);
    }

    public boolean hasControlDependence(GraphNode<?> a, GraphNode<?> b) {
        int yes = 0;
        List<Arc<ArcData>> list = a.getOutgoingArcs();
        // Nodes with less than 1 outgoing arc cannot control another node.
        if (list.size() < 2)
            return false;
        for (Arc<ArcData> arc : list) {
            GraphNode<?> successor = arc.getToNode();
            if (postdominates(successor, b))
                yes++;
        }
        int no = list.size() - yes;
        return yes > 0 && no > 0;
    }

    public boolean postdominates(GraphNode<?> a, GraphNode<?> b) {
        return postdominates(a, b, new HashSet<>());
    }

    private boolean postdominates(GraphNode<?> a, GraphNode<?> b, Set<GraphNode<?>> visited) {
        // Stop w/ success if a == b or a has already been visited
        if (a.equals(b) || visited.contains(a))
            return true;
        List<Arc<ArcData>> outgoing = a.getOutgoingArcs();
        // Limit the traversal if it is a PPDG
        if (pdg instanceof PPDG)
            outgoing = outgoing.stream().filter(Arc::isExecutableControlFlowArrow).collect(Collectors.toList());
        // Stop w/ failure if there are no edges to traverse from a
        if (outgoing.size() == 0)
            return false;
        // Find all possible paths starting from a, if ALL find b, then true, else false
        visited.add(a);
        for (Arc<ArcData> out : outgoing) {
            if (!postdominates(out.getToNode(), b, visited))
                return false;
        }
        return true;
    }
}
