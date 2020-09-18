package es.upv.mist.slicing.graphs.augmented;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.graphs.pdg.PDG;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** A pseudo-predicate PDG, equivalent to an APDG that is built using the {@link PPControlDependencyBuilder
 * pseudo-predicate control dependency algorithm} instead of the classic one. */
public class PPDG extends APDG {
    public PPDG() {
        this(new ACFG());
    }

    public PPDG(ACFG acfg) {
        super(acfg);
    }

    @Override
    protected PDG.Builder createBuilder() {
        return new Builder();
    }

    /** Populates a PPDG.
     * @see APDG.Builder
     * @see PPControlDependencyBuilder */
    public class Builder extends APDG.Builder {
        protected Builder() {
            super();
        }

        @Override
        public void build(CallableDeclaration<?> declaration) {
            super.build(declaration);
            markPPDGExclusiveEdges(declaration);
        }

        @Override
        protected void buildControlDependency() {
            new PPControlDependencyBuilder((ACFG) cfg, PPDG.this).build();
        }

        /** Finds the CD arcs that are only present in the PPDG and marks them as such. */
        protected void markPPDGExclusiveEdges(CallableDeclaration<?> declaration) {
            APDG apdg = new APDG((ACFG) cfg);
            apdg.build(declaration);
            Set<Arc> apdgArcs = apdg.edgeSet().stream()
                    .filter(Arc::isUnconditionalControlDependencyArc)
                    .collect(Collectors.toSet());
            edgeSet().stream()
                    .filter(Arc::isUnconditionalControlDependencyArc)
                    .filter(Predicate.not(apdgArcs::contains))
                    .map(Arc::asControlDependencyArc)
                    .forEach(ControlDependencyArc::setPPDGExclusive);
        }
    }
}
