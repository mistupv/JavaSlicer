package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.VariableAction;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * An arc used in the {@link PDG} and {@link SDG},
 * representing the declaration of some data linked to its usage (of that value).
 * There is data dependency between two nodes if and only if (1) the source <it>may</it>
 * declare a variable, (2) the destination <it>may</it> use it, and (3) there is a
 * path between the nodes where the variable is not redefined.
 * <br/>
 * Data dependency arcs are specific to a DEC-DEF or DEF-USE combination, and can
 * be easily identified by source or target {@link VariableAction}.
 */
public class DataDependencyArc extends Arc {
    /** Valid combinations of variable actions. */
    private static final List<BiPredicate<VariableAction, VariableAction>> VALID_VA_COMBOS =
            List.of((a, b) -> a.isDefinition() && b.isUsage(), (a, b) -> a.isDeclaration() && b.isDefinition());

    protected final VariableAction sourceVar;
    protected final VariableAction targetVar;

    public DataDependencyArc(VariableAction sourceVar, VariableAction targetVar) {
        super(sourceVar.getName());
        if (VALID_VA_COMBOS.stream().noneMatch(p -> p.test(sourceVar, targetVar)))
            throw new IllegalArgumentException("Illegal combination of actions: " + sourceVar + ", " + targetVar);
        this.sourceVar = sourceVar;
        this.targetVar = targetVar;
    }

    public VariableAction getSourceVar() {
        return sourceVar;
    }

    public VariableAction getTargetVar() {
        return targetVar;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
                && o instanceof DataDependencyArc
                && sourceVar == ((DataDependencyArc) o).sourceVar
                && targetVar == ((DataDependencyArc) o).targetVar;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceVar, targetVar);
    }

}

