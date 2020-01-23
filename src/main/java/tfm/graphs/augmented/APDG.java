package tfm.graphs.augmented;

import tfm.graphs.PDG;

public class APDG extends PDG {
    public APDG() {
        this(new ACFG());
    }

    public APDG(ACFG acfg) {
        super(acfg);
    }
}
