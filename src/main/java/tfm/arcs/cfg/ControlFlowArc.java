package tfm.arcs.cfg;

import tfm.arcs.Arc;
import tfm.arcs.data.VoidArcData;
import tfm.nodes.GraphNode;

/**
 * An edge of the {@link tfm.graphs.CFG}, representing the direct
 * flow of control. It connects two instructions if, when the source
 * is executed, one of the possible next instructions is the destination.
 */
public class ControlFlowArc extends Arc<VoidArcData> {

    public ControlFlowArc(GraphNode<?> from, GraphNode<?> to) {
        super(from, to);
    }

    @Override
    public boolean isControlFlowArrow() {
        return true;
    }

    @Override
    public boolean isExecutableControlFlowArrow() {
        return true;
    }

    @Override
    public boolean isControlDependencyArrow() {
        return false;
    }

    @Override
    public boolean isDataDependencyArrow() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("ControlFlowArc{%s -> %s}",
                getFromNode().getId(),
                getToNode().getId()
        );
    }

    /**
     * Represents a non-executable control flow arc, used within the {@link tfm.graphs.CFG.ACFG ACFG}.
     * Initially it had the following meaning: connecting a statement with
     * the following one as if the source was a {@code nop} command (no operation).
     * <br/>
     * It is used to improve control dependence, and it should be skipped when
     * computing data dependence and other analyses.
     */
    public final static class NonExecutable extends ControlFlowArc {
        public NonExecutable(GraphNode<?> from, GraphNode<?> to) {
            super(from, to);
        }

        @Override
        public String toString() {
            return "NonExecutable" + super.toString();
        }

        @Override
        public boolean isExecutableControlFlowArrow() {
            return false;
        }

        @Override
        public String toGraphvizRepresentation() {
            return super.toGraphvizRepresentation() + "[style = dashed]";
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof NonExecutable && super.equals(o);
        }
    }
}
