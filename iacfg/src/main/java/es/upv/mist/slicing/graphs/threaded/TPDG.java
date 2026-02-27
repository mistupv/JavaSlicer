package es.upv.mist.slicing.graphs.threaded;

import es.upv.mist.slicing.graphs.augmented.PPDG;

public class TPDG extends PPDG {
    @Override
    protected Builder createBuilder() {
        return new Builder();
    }

    public class Builder extends PPDG.Builder {

    }
}
