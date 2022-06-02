package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.utils.Utils;

/** Represents a data dependency in an object-oriented SDG or PDG. */
public class FlowDependencyArc extends Arc {
    public FlowDependencyArc() {
        super();
    }

    public FlowDependencyArc(String variable) {
        super(variable);
    }

    public FlowDependencyArc(String[] member) {
        super(Utils.arrayJoin(member, "."));
    }

}
