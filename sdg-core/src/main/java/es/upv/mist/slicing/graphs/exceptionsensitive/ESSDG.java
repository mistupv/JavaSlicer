package es.upv.mist.slicing.graphs.exceptionsensitive;

import es.upv.mist.slicing.arcs.sdg.ReturnArc;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.augmented.PPDG;
import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.exceptionsensitive.ExitNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ReturnNode;
import es.upv.mist.slicing.slicing.ExceptionSensitiveSlicingAlgorithm;
import es.upv.mist.slicing.slicing.SlicingAlgorithm;

/** An exception-sensitive SDG, equivalent to an PSDG, that is built using the {@link ESPDG}
 *  instead of {@link PPDG}. It features a different slicing algorithm
 *  and return arcs, which connect an exit node to a return node. */
public class ESSDG extends PSDG {
    @Override
    protected Builder createBuilder() {
        return new Builder();
    }

    @Override
    protected SlicingAlgorithm createSlicingAlgorithm() {
        return new ExceptionSensitiveSlicingAlgorithm(this);
    }

    public void addReturnArc(ExitNode source, ReturnNode target) {
        addEdge(source, target, new ReturnArc());
    }

    /** Populates an ESSDG, using ESPDG and ESCFG as default graphs.
     * @see PSDG.Builder
     * @see ExceptionSensitiveCallConnector */
    protected class Builder extends PSDG.Builder {
        @Override
        protected CFG createCFG() {
            return new ESCFG();
        }

        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof ESCFG;
            return new ESPDG((ESCFG) cfg);
        }

        @Override
        protected void connectCalls(CallGraph callGraph) {
            new ExceptionSensitiveCallConnector(ESSDG.this).connectAllCalls(callGraph);
        }
    }
}
