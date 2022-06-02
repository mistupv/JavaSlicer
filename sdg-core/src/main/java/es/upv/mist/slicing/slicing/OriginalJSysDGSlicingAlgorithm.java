package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ObjectFlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.TotalDefinitionDependenceArc;
import es.upv.mist.slicing.arcs.sdg.ParameterInOutArc;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;

public class OriginalJSysDGSlicingAlgorithm extends JSysDGSlicingAlgorithm {
    public OriginalJSysDGSlicingAlgorithm(JSysDG graph) {
        super(graph);
    }

    @Override
    protected boolean commonIgnoreConditions(Arc arc) {
        return arc instanceof ParameterInOutArc.ObjectFlow ||
                arc instanceof ObjectFlowDependencyArc ||
                arc instanceof TotalDefinitionDependenceArc ||
                ppdgIgnore(arc) || essdgIgnore(arc);
    }
}
