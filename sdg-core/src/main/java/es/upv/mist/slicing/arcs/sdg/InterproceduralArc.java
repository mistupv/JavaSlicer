package es.upv.mist.slicing.arcs.sdg;

import es.upv.mist.slicing.arcs.Arc;

/** An arc that connects nodes from different procedures. Depending on the source and target, it can be considered
 * an input (connects a call to a declaration) or output (connects the end of the declaration back to the call). */
public abstract class InterproceduralArc extends Arc {
    protected InterproceduralArc() {
        super();
    }

    @Override
    public abstract boolean isInterproceduralInputArc();

    @Override
    public abstract boolean isInterproceduralOutputArc();
}
