package tfm.arcs.pdg;

import tfm.arcs.Arc;

public class ControlDependencyArc extends Arc {

    public ControlDependencyArc(String variable) {
        super(variable);
    }

    public ControlDependencyArc() {
    }

    @Override
    public boolean isControlFlowArrow() {
        return false;
    }

    @Override
    public boolean isControlDependencyArrow() {
        return true;
    }

    @Override
    public boolean isDataDependencyArrow() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("ControlDependencyArc{%s}", super.toString());
    }
}
