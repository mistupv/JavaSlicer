package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;

/** Represents a data dependency in an object-oriented SDG or PDG. */
public class FlowDependencyArc extends Arc {
    public FlowDependencyArc() {
        super();
    }

    public FlowDependencyArc(String variable) {
        super(variable);
    }

}
