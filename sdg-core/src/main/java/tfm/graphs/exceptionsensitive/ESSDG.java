package tfm.graphs.exceptionsensitive;

import tfm.arcs.sdg.ReturnArc;
import tfm.graphs.CallGraph;
import tfm.graphs.augmented.PSDG;
import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.nodes.exceptionsensitive.ExitNode;
import tfm.nodes.exceptionsensitive.ReturnNode;
import tfm.slicing.ExceptionSensitiveSlicingAlgorithm;
import tfm.slicing.SlicingAlgorithm;

/** An exception-sensitive SDG, equivalent to an PSDG, that is built using the {@link ESPDG}
 *  instead of {@link tfm.graphs.augmented.PPDG}. It features a different slicing algorithm
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
    class Builder extends PSDG.Builder {
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
