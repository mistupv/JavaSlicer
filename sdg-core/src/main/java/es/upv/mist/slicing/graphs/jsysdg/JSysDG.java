package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import es.upv.mist.slicing.arcs.sdg.ReturnArc;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ExceptionSensitiveCallConnector;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.exceptionsensitive.ExitNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ReturnNode;
import es.upv.mist.slicing.slicing.ExceptionSensitiveSlicingAlgorithm;
import es.upv.mist.slicing.slicing.SlicingAlgorithm;

public class JSysDG extends ESSDG {

    @Override
    protected JSysDG.Builder createBuilder() {
        return new JSysDG.Builder();
    }

    @Override
    protected SlicingAlgorithm createSlicingAlgorithm() {
        return new ExceptionSensitiveSlicingAlgorithm(this);
    }

    public void addReturnArc(ExitNode source, ReturnNode target) {
        addEdge(source, target, new ReturnArc());
    }

    /** Populates an ESSDG, using ESPDG and ESCFG as default graphs.
     * @see PSDG.Builder
     * @see ExceptionSensitiveCallConnector */
    class Builder extends ESSDG.Builder {

        private ClassGraph classGraph;

        @Override
        public void build(NodeList<CompilationUnit> nodeList) {
            // See creation strategy at http://kaz2.dsic.upv.es:3000/Fzg46cQvT1GzHQG9hFnP1g#Using-data-flow-in-the-SDG
            classGraph = createClassGraph(nodeList);
            buildCFGs(nodeList);                             // 1
            CallGraph callGraph = createCallGraph(nodeList); // 2
            dataFlowAnalysis(callGraph);                     // 3
            buildAndCopyPDGs();                              // 4
            connectCalls(callGraph);                         // 5
            createSummaryArcs(callGraph);                    // 6
        }

        @Override
        protected CFG createCFG() {
            return new JSysCFG(classGraph);
        }

        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof JSysCFG;
            return new JSysPDG((JSysCFG) cfg);
        }
    }
}
