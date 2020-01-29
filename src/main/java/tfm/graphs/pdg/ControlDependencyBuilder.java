package tfm.graphs.pdg;

import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;

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
class ControlDependencyBuilder {
    private final PDG pdg;
    private final CFG cfg;

    public ControlDependencyBuilder(PDG pdg, CFG cfg) {
        this.pdg = pdg;
        this.cfg = cfg;
    }

    public void analyze() {
        assert cfg.getRootNode().isPresent();
        assert pdg.getRootNode().isPresent();

        boolean needsStartEndEdge = !cfg.containsEdge(cfg.getRootNode().get(), cfg.getExitNode());
        if (needsStartEndEdge)
            cfg.addControlFlowEdge(cfg.getRootNode().get(), cfg.getExitNode());

        PostdominatorTree tree = new PostdominatorTree(cfg);

        for (GraphNode<?> src : pdg.vertexSet())
            for (GraphNode<?> dest : tree.controlDependenciesOf(cfg, src))
                if (!src.equals(dest))
                    pdg.addControlDependencyArc(src, dest);

        if (needsStartEndEdge)
            cfg.removeEdge(cfg.getRootNode().get(), cfg.getExitNode());
    }
}
