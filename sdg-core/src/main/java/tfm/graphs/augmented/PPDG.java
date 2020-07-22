package tfm.graphs.augmented;

import tfm.graphs.pdg.PDG;

public class PPDG extends APDG {
    public PPDG() {
        this(new ACFG());
    }

    public PPDG(ACFG acfg) {
        super(acfg);
    }

    @Override
    protected PDG.Builder createBuilder() {
        return new Builder();
    }

    public class Builder extends APDG.Builder {
        protected Builder() {
            super();
        }

        @Override
        protected void buildControlDependency() {
            new ControlDependencyBuilder((ACFG) cfg, PPDG.this).build();
        }
    }
}
