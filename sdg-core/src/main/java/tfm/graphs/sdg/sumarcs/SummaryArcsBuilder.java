package tfm.graphs.sdg.sumarcs;

import tfm.graphs.sdg.SDG;

public abstract class SummaryArcsBuilder {

    protected SDG sdg;

    protected SummaryArcsBuilder(SDG sdg) {
        this.sdg = sdg;
    }

    public abstract void visit();
}
