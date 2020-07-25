package tfm.graphs.exceptionsensitive;

import tfm.arcs.pdg.ConditionalControlDependencyArc;
import tfm.graphs.augmented.PPDG;
import tfm.graphs.pdg.PDG;
import tfm.nodes.GraphNode;

public class ESPDG extends PPDG {
    public ESPDG() {
        this(new ESCFG());
    }

    public ESPDG(ESCFG escfg) {
        super(escfg);
    }

    public void addCC1Arc(GraphNode<?> src, GraphNode<?> dst) {
        addEdge(src, dst, new ConditionalControlDependencyArc.CC1());
    }

    public void addCC2Arc(GraphNode<?> src, GraphNode<?> dst) {
        addEdge(src, dst, new ConditionalControlDependencyArc.CC2());
    }

    @Override
    protected PDG.Builder createBuilder() {
        return new Builder();
    }

    public class Builder extends PPDG.Builder {
        protected Builder() {
            super();
        }

        @Override
        protected void buildControlDependency() {
            super.buildControlDependency();
            new ConditionalControlDependencyBuilder((ESCFG) cfg, ESPDG.this).build();
        }
    }
}
