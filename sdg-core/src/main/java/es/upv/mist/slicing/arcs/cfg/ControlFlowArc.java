package es.upv.mist.slicing.arcs.cfg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.augmented.ACFG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

/**
 * An edge of the {@link CFG}, representing the direct
 * flow of control. It connects two instructions if, when the source
 * is executed, one of the possible next instructions is the destination.
 */
public class ControlFlowArc extends Arc {
    /**
     * Represents a non-executable control flow arc, used within the {@link ACFG ACFG}.
     * Initially it had the following meaning: connecting a statement with
     * the following one as if the source was a {@code nop} command (no operation).
     * <br/>
     * It is used to improve control dependence, and it should be skipped when
     * computing data dependence and other analyses.
     * @see ACFG
     * @see ControlFlowArc
     */
    public static final class NonExecutable extends ControlFlowArc {
        @Override
        public Map<String, Attribute> getDotAttributes() {
            Map<String, Attribute> map = super.getDotAttributes();
            map.put("style", DefaultAttribute.createAttribute("dashed"));
            return map;
        }
    }
}
