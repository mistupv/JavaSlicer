package tfm.graphs.augmented;

import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.cfg.CFG;
import tfm.graphs.cfg.CFGBuilder;
import tfm.nodes.GraphNode;

public class ACFG extends CFG {
    public void addNonExecutableControlFlowEdge(GraphNode<?> from, GraphNode<?> to) {
        addControlFlowEdge(from, to, new ControlFlowArc.NonExecutable());
    }

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new ACFGBuilder(this);
    }
}
