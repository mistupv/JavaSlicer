package es.upv.mist.slicing.graphs.icfg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.cli.DOTAttributes;
import es.upv.mist.slicing.cli.GraphLog;

public class ICFGLog extends GraphLog<ICFG> {
    public ICFGLog() {
        super();
    }

    public ICFGLog(ICFG graph) {
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
