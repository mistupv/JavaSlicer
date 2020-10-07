package es.upv.mist.slicing.graphs.augmented;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An alternative program dependence graph to the {@link PPDG}, which
 * performs the same optimization without altering the slicing algorithm,
 * thus guaranteeing that it's kept ats its original linear time complexity.
 */
public class TapasPDG extends APDG {
    public TapasPDG() {
        super();
    }

    public TapasPDG(ACFG acfg) {
        super(acfg);
    }

    @Override
    protected Builder createBuilder() {
        return new Builder();
    }

    public class Builder extends APDG.Builder {
        @Override
        protected void buildControlDependency() {
            super.buildControlDependency();
            algorithm1();
        }

        /** Removes all incoming edges of a pseudo-predicate, when (1) there is a control
         *  dependency between itself and another pseudo-predicate, and (2) both jump to
         *  the same instruction. */
        protected void algorithm1() {
            Set<ControlDependencyArc> Ac = edgeSet().stream()
                    .filter(Arc::isControlDependencyArc)
                    .map(ControlDependencyArc.class::cast)
                    .collect(Collectors.toSet());
            Set<Arc> arcsToRemove = new HashSet<>();
            for (ControlDependencyArc arc : Ac) {
                GraphNode<?> ns = getEdgeSource(arc);
                GraphNode<?> ne = getEdgeTarget(arc);
                ACFG acfg = (ACFG) cfg;
                if (acfg.isPseudoPredicate(ns) && acfg.isPseudoPredicate(ne) && jumpDest(ns).equals(jumpDest(ne))) {
                    edgeSet().stream()
                            .filter(Arc::isControlDependencyArc)
                            .filter(a -> getEdgeTarget(a).equals(ne))
                            .forEach(arcsToRemove::add);
                }
            }
            arcsToRemove.forEach(TapasPDG.this::removeEdge);
        }

        /** Obtain the target of a jump instruction. In the CFG, it should
         *  have just one executable outgoing edge. */
        protected GraphNode<?> jumpDest(GraphNode<?> jumpNode) {
            Set<Arc> outgoing = new HashSet<>(cfg.outgoingEdgesOf(jumpNode));
            outgoing.removeIf(Arc::isNonExecutableControlFlowArc);
            if (outgoing.size() != 1)
                throw new IllegalArgumentException("Jump has 0 or multiple executable outgoing edges.");
            return cfg.getEdgeTarget(outgoing.iterator().next());
        }
    }
}
