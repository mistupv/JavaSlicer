package tfm.arcs.pdg;

import tfm.arcs.Arc;
import tfm.arcs.data.VariableArcData;
import tfm.nodes.GraphNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An arc used in the {@link tfm.graphs.PDG} and {@link tfm.graphs.SDG},
 * representing the declaration of some data linked to its usage (of that value).
 * There is data dependency between two nodes if and only if (1) the source <it>may</it>
 * declare a variable, (2) the destination <it>may</it> use it, and (3) there is a
 * path between the nodes where the variable is not redefined.
 */
public class DataDependencyArc extends Arc<VariableArcData> {

    public DataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable, String... variables) {
        super(from, to);

        List<String> variablesList = new ArrayList<>(variables.length + 1);

        variablesList.add(variable);
        variablesList.addAll(Arrays.asList(variables));

        VariableArcData variableArcData = new VariableArcData(variablesList);

        setData(variableArcData);
    }

    @Override
    public boolean isControlFlowArrow() {
        return false;
    }

    @Override
    public boolean isExecutableControlFlowArrow() {
        return false;
    }

    @Override
    public boolean isControlDependencyArrow() {
        return false;
    }

    @Override
    public boolean isDataDependencyArrow() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("DataDependencyArc{%s, %s -> %s}",
                getData(),
                getFromNode().getId(),
                getToNode().getId());
    }

    @Override
    public String toGraphvizRepresentation() {
        return String.format("%s [style=dashed, color=red, label=\"%s\"];", super.toGraphvizRepresentation(), getData().toString());
    }
}

