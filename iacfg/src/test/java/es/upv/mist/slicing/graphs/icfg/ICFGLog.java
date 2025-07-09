package es.upv.mist.slicing.graphs.icfg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.cli.DOTAttributes;
import es.upv.mist.slicing.cli.GraphLog;
import es.upv.mist.slicing.nodes.GraphNode;

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

    @Override
    protected DOTAttributes vertexAttributes(GraphNode<?> node) {
        DOTAttributes res = new DOTAttributes();
        res.set("label", node.getLongLabel() + "\n" + this.graph.getTopologicalNumbersString(node));
        if (node.isImplicitInstruction())
            res.add("style", "dashed");
        return res;
    }
}
