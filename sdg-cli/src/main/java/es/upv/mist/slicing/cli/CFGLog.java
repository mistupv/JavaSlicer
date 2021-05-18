package es.upv.mist.slicing.cli;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.cfg.CFG;

public class CFGLog extends GraphLog<CFG> {
    public CFGLog() {
        super();
    }

    public CFGLog(CFG graph) {
        super(graph);
    }

    @Override
    protected DOTAttributes edgeAttributes(Arc arc) {
        DOTAttributes res = super.edgeAttributes(arc);
        if (arc.isNonExecutableControlFlowArc())
            res.add("style", "dashed");
        return res;
    }
}
