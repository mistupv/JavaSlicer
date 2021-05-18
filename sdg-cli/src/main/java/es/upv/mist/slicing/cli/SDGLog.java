package es.upv.mist.slicing.cli;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.sdg.InterproceduralArc;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.slicing.Slice;

public class SDGLog extends GraphLog<SDG> {
    protected final Slice slice;

    public SDGLog(SDG graph) {
        this(graph, null);
    }

    public SDGLog(SDG graph, Slice slice) {
        super(graph);
        this.slice = slice;
    }

    @Override
    protected DOTAttributes vertexAttributes(GraphNode<?> node) {
        DOTAttributes res = super.vertexAttributes(node);
        if (slice != null) {
            if (slice.contains(node))
                res.add("style", "filled");
            if (slice.getCriterion().contains(node)) {
                res.add("style", "filled");
                res.set("fillcolor", "lightblue");
            }
        }
        return res;
    }

    @Override
    protected DOTAttributes edgeAttributes(Arc arc) {
        DOTAttributes res = PDGLog.pdgEdgeAttributes(arc);
        if (arc instanceof InterproceduralArc || arc.isSummaryArc())
            res.set("penwidth", "3");
        if (arc.isObjectFlow() && arc instanceof InterproceduralArc)
            res.add("style", "dotted");
        if (arc.isInterproceduralInputArc())
            res.set("color", "darkgreen");
        if (arc.isInterproceduralOutputArc())
            res.set("color", "blue");
        if (arc.isSummaryArc())
            res.set("color", "#a01210");
        return res;
    }
}
