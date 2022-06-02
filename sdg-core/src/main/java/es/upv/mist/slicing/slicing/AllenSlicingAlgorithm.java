package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;

public class AllenSlicingAlgorithm extends JSysDGSlicingAlgorithm {

    public AllenSlicingAlgorithm(JSysDG graph) {
        super(graph);
    }

    @Override
    protected boolean commonIgnoreConditions(Arc arc) {
        return arc instanceof ConditionalControlDependencyArc.CC1 ||
                arc instanceof ConditionalControlDependencyArc.CC2 ||
                objectFlowIgnore(arc) ||
                ppdgIgnore(arc);
    }
}
