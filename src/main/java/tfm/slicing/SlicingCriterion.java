package tfm.slicing;

import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.GraphNode;

import java.util.Optional;

public abstract class SlicingCriterion {

    protected String variable;

    public SlicingCriterion(String variable) {
        this.variable = variable;
    }

    public String getVariable() {
        return variable;
    }

    public abstract Optional<GraphNode<?>> findNode(CFGGraph graph);
    public abstract Optional<GraphNode<?>> findNode(PDGGraph graph);
    public abstract Optional<GraphNode<?>> findNode(SDGGraph graph);

    @Override
    public String toString() {
        return "(" + variable + ")";
    }
}
