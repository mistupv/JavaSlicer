package es.upv.mist.slicing.graphs.augmented;

import es.upv.mist.slicing.graphs.pdg.PDG;

/** A pseudo-predicate PDG, equivalent to an APDG that is built using the {@link PPControlDependencyBuilder
 * pseudo-predicate control dependency algorithm} instead of the classic one. */
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

    /** Populates a PPDG.
     * @see APDG.Builder
     * @see PPControlDependencyBuilder */
    public class Builder extends APDG.Builder {
        protected Builder() {
            super();
        }

        @Override
        protected void buildControlDependency() {
            new PPControlDependencyBuilder((ACFG) cfg, PPDG.this).build();
        }
    }
}
