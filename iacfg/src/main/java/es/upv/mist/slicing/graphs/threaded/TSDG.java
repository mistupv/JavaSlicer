package es.upv.mist.slicing.graphs.threaded;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import es.upv.mist.slicing.arcs.InterferenceDependencyArc;
import es.upv.mist.slicing.graphs.augmented.ASDG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.icfg.ICFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;

public class TSDG extends ASDG {
    protected final ICFG icfg;

    public TSDG() {
        this.icfg = new ICFG();
    }

    public TSDG(ICFG icfg) {
        this.icfg = icfg;
    }

    public void addInterferenceDependencyArc(VariableAction source, VariableAction target) {
        GraphNode<?> src = source.getGraphNode();
        GraphNode<?> tgt = target.getGraphNode();
        addEdge(src, tgt, new InterferenceDependencyArc(source, target));
    }

    @Override
    protected ASDG.Builder createBuilder() {
        return new Builder();
    }

    public class Builder extends ASDG.Builder {
        public void build(NodeList<CompilationUnit> nodeList) {
            super.build(nodeList);
            icfg.build(cfgMap, callGraph);
            computeInterferences();
        }

        @Override
        protected PDG createPDG(CFG cfg) {
            return new TPDG();
        }

        protected void computeInterferences() {
            // TODO: actually compute
        }
    }
}
