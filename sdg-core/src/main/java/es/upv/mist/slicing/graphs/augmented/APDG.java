package es.upv.mist.slicing.graphs.augmented;

import es.upv.mist.slicing.graphs.pdg.PDG;

/** An Augmented PDG, which is equivalent to a PDG that is built based on an Augmented CFG instead of
 * the PDG. The use of this graph is deprecated, as the {@link PPDG} produces far smaller slices. */
public class APDG extends PDG {
    public APDG() {
        this(new ACFG());
    }

    public APDG(ACFG acfg) {
        super(acfg);
    }
}
