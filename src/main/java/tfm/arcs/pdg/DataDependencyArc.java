package tfm.arcs.pdg;

import tfm.arcs.Arc;
public class DataDependencyArc extends Arc {

    public DataDependencyArc() {
    }

    public DataDependencyArc(String variable) {
        super(variable);
    }

    @Override
    public boolean isControlFlowArrow() {
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
        return String.format("DataDependencyArc{%s}", super.toString());
    }

    @Override
    public String toGraphvizRepresentation() {
        return String.format("%s [style=dashed, color=red%s];",
                super.toGraphvizRepresentation(),
                getVariable().map(variable -> String.format(", label=\"%s\"", variable)).orElse("")
        );
    }
}

