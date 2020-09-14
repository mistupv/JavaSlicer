package es.upv.mist.slicing.arcs.sdg;

import es.upv.mist.slicing.nodes.exceptionsensitive.ExitNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ReturnNode;

/** An interprocedural arc that connects a {@link ExitNode method exit node} to a
 *  {@link ReturnNode method call return node}. This arc is always an output. */
public class ReturnArc extends InterproceduralArc {
    @Override
    public boolean isInterproceduralInputArc() {
        return false;
    }

    @Override
    public boolean isInterproceduralOutputArc() {
        return true;
    }
}
