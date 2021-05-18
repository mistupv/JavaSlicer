package es.upv.mist.slicing.arcs;

import es.upv.mist.slicing.arcs.cfg.ControlFlowArc;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.DataDependencyArc;
import es.upv.mist.slicing.arcs.sdg.CallArc;
import es.upv.mist.slicing.arcs.sdg.ParameterInOutArc;
import es.upv.mist.slicing.arcs.sdg.SummaryArc;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;
import org.jgrapht.graph.DefaultEdge;

import java.util.Objects;

/** The root class from which all arcs in the {@link CFG CFG}, {@link PDG PDG}
 * and {@link SDG SDG} inherit. */
public abstract class Arc extends DefaultEdge {
    protected final String label;

    protected Arc() {
        this(null);
    }

    protected Arc(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    // =========================== CFG ===========================

    /** @see ControlFlowArc */
    public final boolean isControlFlowArc() {
        return this instanceof ControlFlowArc;
    }

    /** @see ControlFlowArc.NonExecutable */
    public final boolean isExecutableControlFlowArc() {
        return isControlFlowArc() && !isNonExecutableControlFlowArc();
    }

    /** @see ControlFlowArc.NonExecutable */
    public final boolean isNonExecutableControlFlowArc() {
        return this instanceof ControlFlowArc.NonExecutable;
    }

    // ======================== PDG & SDG ========================

    /** @see ControlDependencyArc */
    public final boolean isControlDependencyArc() {
        return this instanceof ControlDependencyArc;
    }

    public final ControlDependencyArc asControlDependencyArc() {
        if (isControlDependencyArc()) return (ControlDependencyArc) this;
        throw new UnsupportedOperationException("Not a ControlDependencyArc");
    }

    /** @see ConditionalControlDependencyArc */
    public final boolean isConditionalControlDependencyArc() {
        return this instanceof ConditionalControlDependencyArc;
    }

    /** @see ConditionalControlDependencyArc */
    public final boolean isUnconditionalControlDependencyArc() {
        return isControlDependencyArc() && !isConditionalControlDependencyArc();
    }

    public final ControlDependencyArc asConditionalControlDependencyArc() {
        if (isConditionalControlDependencyArc()) return (ConditionalControlDependencyArc) this;
        throw new UnsupportedOperationException("Not a ConditionalControlDependencyArc");
    }

    /** @see DataDependencyArc */
    public final boolean isDataDependencyArc() {
        return this instanceof DataDependencyArc;
    }

    public final DataDependencyArc asDataDependencyArc() {
        if (isDataDependencyArc()) return (DataDependencyArc) this;
        throw new UnsupportedOperationException("Not a DataDependencyArc");
    }

    public boolean isObjectFlow() {
        return false;
    }

    // =========================== SDG ===========================

    /** Whether or not this is an interprocedural arc that connects a call site to its declaration. */
    public boolean isInterproceduralInputArc() {
        return false;
    }

    /** Whether or not this is an interprocedural arc that connects a declaration to a matching call site. */
    public boolean isInterproceduralOutputArc() {
        return false;
    }

    /** @see CallArc */
    public final boolean isCallArc() {
        return this instanceof CallArc;
    }

    public final CallArc asCallArc() {
        if (isCallArc()) return (CallArc) this;
        throw new UnsupportedOperationException("Not a CallArc");
    }

    /** @see ParameterInOutArc */
    public final boolean isParameterInOutArc() {
        return this instanceof ParameterInOutArc;
    }

    public final ParameterInOutArc asParameterInOutArc() {
        if (isParameterInOutArc()) return (ParameterInOutArc) this;
        throw new UnsupportedOperationException("Not a ParameterInOutArc");
    }

    /** @see SummaryArc */
    public final boolean isSummaryArc() {
        return this instanceof SummaryArc;
    }

    public final SummaryArc asSummaryArcArc() {
        if (isSummaryArc()) return (SummaryArc) this;
        throw new UnsupportedOperationException("Not a SummaryArc");
    }

    @Override
    public String toString() {
        return String.format("%s{%d -> %d}", getClass().getName(),
                ((GraphNode<?>) super.getSource()).getId(), ((GraphNode<?>) super.getTarget()).getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (!o.getClass().equals(this.getClass()))
            return false;
        return Objects.equals(getSource(), ((Arc) o).getSource())
                && Objects.equals(getTarget(), ((Arc) o).getTarget())
                && Objects.equals(getLabel(), ((Arc) o).getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), getLabel(), getSource(), getTarget());
    }
}
