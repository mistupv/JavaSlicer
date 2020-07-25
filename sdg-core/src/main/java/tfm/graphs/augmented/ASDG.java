package tfm.graphs.augmented;

import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;

/** An 'Augmented SDG', equivalent to the SDG which is equivalent to an SDG that is built based on an Augmented
 * PDG instead of the PDG. The use of this graph is deprecated, as the {@link PSDG} produces far smaller slices. */
public class ASDG extends SDG {
    @Override
    protected Builder createBuilder() {
        return new Builder();
    }

    /** Populates an ASDG, using {@link ACFG} and {@link APDG} as default graphs.
     * @see SDG.Builder */
    public class Builder extends SDG.Builder {
        @Override
        protected CFG createCFG() {
            return new ACFG();
        }

        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof ACFG;
            return new APDG((ACFG) cfg);
        }
    }
}
