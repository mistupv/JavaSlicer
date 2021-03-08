package es.upv.mist.slicing.arcs.sdg;

/**
 * An interprocedural arc that connects a call site with its
 * corresponding declaration. It is considered an interprocedural input.
 */
public class CallArc extends InterproceduralArc {
    @Override
    public boolean isInterproceduralInputArc() {
        return true;
    }

    @Override
    public boolean isInterproceduralOutputArc() {
        return false;
    }
}
