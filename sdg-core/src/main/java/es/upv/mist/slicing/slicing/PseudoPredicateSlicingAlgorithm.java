package es.upv.mist.slicing.slicing;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.nodes.GraphNode;

public class PseudoPredicateSlicingAlgorithm extends ClassicSlicingAlgorithm {
    protected GraphNode<?> slicingCriterion;

    public PseudoPredicateSlicingAlgorithm(PSDG graph) {
        super(graph);
    }

    @Override
    public Slice traverseProcedure(GraphNode<?> slicingCriterion) {
        this.slicingCriterion = slicingCriterion;
        return super.traverseProcedure(slicingCriterion);
    }

    @Override
    public Slice traverse(GraphNode<?> slicingCriterion) {
        this.slicingCriterion = slicingCriterion;
        return super.traverse(slicingCriterion);
    }

    @Override
    protected boolean ignorePass1(Arc arc) {
        return super.ignorePass1(arc) || ignorePseudoPredicate(arc);
    }

    @Override
    protected boolean ignorePass2(Arc arc) {
        return super.ignorePass2(arc) || ignorePseudoPredicate(arc);
    }

    @Override
    public boolean ignoreProcedure(Arc arc) {
        return super.ignoreProcedure(arc) || ignorePseudoPredicate(arc);
    }

    protected boolean ignorePseudoPredicate(Arc arc) {
        GraphNode<?> target = graph.getEdgeTarget(arc);
        return ((PSDG) graph).isPseudoPredicate(target)
                && arc.isControlDependencyArc()
                && target != slicingCriterion;
    }
}
