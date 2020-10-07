package es.upv.mist.slicing.graphs.augmented;

import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;

public class TapasSDG extends ASDG {
    @Override
    protected Builder createBuilder() {
        return new Builder();
    }

    public class Builder extends ASDG.Builder {
        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof ACFG;
            return new TapasPDG((ACFG) cfg);
        }
    }
}
