package tfm.slicing;

import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.nodes.SDGNode;

import java.util.Optional;

public abstract class SlicingCriterion {

    protected String variable;

    public SlicingCriterion(String variable) {
        this.variable = variable;
    }

    public String getVariable() {
        return variable;
    }

    public abstract Optional<CFGNode> findNode(CFGGraph graph);
    public abstract Optional<PDGNode> findNode(PDGGraph graph);
    public abstract Optional<SDGNode> findNode(SDGGraph graph);

    @Override
    public String toString() {
        return "(" + variable + ")";
    }
}
