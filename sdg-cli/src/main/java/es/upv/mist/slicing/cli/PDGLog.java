package es.upv.mist.slicing.cli;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.FlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ObjectFlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.StructuralArc;
import es.upv.mist.slicing.arcs.sdg.InterproceduralArc;
import es.upv.mist.slicing.graphs.pdg.PDG;

import java.io.IOException;

public class PDGLog extends GraphLog<PDG> {
    private final CFGLog cfgLog;

    public PDGLog() {
        this(null);
    }

    public PDGLog(PDG pdg) {
        super(pdg);

        if (graph != null && graph.getCfg() != null)
            cfgLog = new CFGLog(graph.getCfg());
        else cfgLog = null;
    }

    @Override
    public void generateImages(String imageName, String format) throws IOException {
        super.generateImages(imageName, format);
        if (cfgLog != null)
            cfgLog.generateImages(imageName, format);
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        super.openVisualRepresentation();

        if (cfgLog != null)
            cfgLog.openVisualRepresentation();
    }

    @Override
    protected DOTAttributes edgeAttributes(Arc arc) {
        return pdgEdgeAttributes(arc);
    }

    public static DOTAttributes pdgEdgeAttributes(Arc arc) {
        DOTAttributes res = new DOTAttributes();
        res.set("label", arc.getLabel());
        if (arc.isDataDependencyArc()
                || arc instanceof FlowDependencyArc
                || arc instanceof ObjectFlowDependencyArc)
            res.set("color", "red");
        if (arc instanceof StructuralArc)
            res.add("style", "dashed");
        if (arc.isObjectFlow() && !(arc instanceof InterproceduralArc))
            res.add("style", "dashed");
        if (arc instanceof ConditionalControlDependencyArc.CC1)
            res.add("color", "orange");
        if (arc instanceof ConditionalControlDependencyArc.CC2)
            res.add("color", "green");
        return res;
    }
}
