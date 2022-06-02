package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.slicing.OriginalJSysDGSlicingAlgorithm;
import es.upv.mist.slicing.slicing.SlicingAlgorithm;

public class OriginalJSysDG extends JSysDG {
    @Override
    protected Builder createBuilder() {
        return new Builder();
    }

    @Override
    protected SlicingAlgorithm createSlicingAlgorithm() {
        return new OriginalJSysDGSlicingAlgorithm(this);
    }

    public class Builder extends JSysDG.Builder {
        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof JSysCFG;
            return new OriginalJSysPDG((JSysCFG) cfg);
        }
    }
}
