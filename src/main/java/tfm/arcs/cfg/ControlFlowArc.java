package tfm.arcs.cfg;

import tfm.arcs.Arc;
import tfm.graphs.augmented.ACFG;

/**
 * An edge of the {@link tfm.graphs.CFG}, representing the direct
 * flow of control. It connects two instructions if, when the source
 * is executed, one of the possible next instructions is the destination.
 */
public class ControlFlowArc extends Arc {
    public ControlFlowArc() {
    }

    /**
     * Represents a non-executable control flow arc, used within the {@link ACFG ACFG}.
     * Initially it had the following meaning: connecting a statement with
     * the following one as if the source was a {@code nop} command (no operation).
     * <br/>
     * It is used to improve control dependence, and it should be skipped when
     * computing data dependence and other analyses.
     */
    public final static class NonExecutable extends ControlFlowArc {
        public NonExecutable() {
        }
    }
}
