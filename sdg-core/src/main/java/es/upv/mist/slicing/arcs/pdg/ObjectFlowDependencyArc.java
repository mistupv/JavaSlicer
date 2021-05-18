package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;

/** Represents a data dependency between objects or between a field
 *  and its parent in an object oriented SDG or PDG. */
public class ObjectFlowDependencyArc extends Arc {
    public ObjectFlowDependencyArc() {
        super();
    }

    @Override
    public boolean isObjectFlow() {
        return true;
    }

}
