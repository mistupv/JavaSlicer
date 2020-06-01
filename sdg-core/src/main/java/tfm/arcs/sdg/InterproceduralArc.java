package tfm.arcs.sdg;

import tfm.arcs.Arc;

public abstract class InterproceduralArc extends Arc {
    protected InterproceduralArc() {
        super();
    }

    @Override
    public abstract boolean isInterproceduralInputArc();

    @Override
    public abstract boolean isInterproceduralOutputArc();
}
