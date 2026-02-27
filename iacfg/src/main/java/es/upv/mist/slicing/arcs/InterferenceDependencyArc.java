package es.upv.mist.slicing.arcs;

import es.upv.mist.slicing.arcs.pdg.DataDependencyArc;
import es.upv.mist.slicing.nodes.VariableAction;

public class InterferenceDependencyArc extends DataDependencyArc {
    public InterferenceDependencyArc(VariableAction sourceVar, VariableAction targetVar) {
        super(sourceVar, targetVar);
    }
}
