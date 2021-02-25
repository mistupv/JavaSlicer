package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ExceptionSensitiveCallConnector;
import es.upv.mist.slicing.graphs.pdg.PDG;

public class JSysDG extends ESSDG {

    @Override
    protected JSysDG.Builder createBuilder() {
        return new JSysDG.Builder();
    }

    /** Populates an ESSDG, using ESPDG and ESCFG as default graphs.
     * @see PSDG.Builder
     * @see ExceptionSensitiveCallConnector */
    class Builder extends ESSDG.Builder {
        @Override
        protected CFG createCFG() {
            return new JSysCFG(classGraph);
        }

        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof JSysCFG;
            return new JSysPDG((JSysCFG) cfg);
        }
    }
}
