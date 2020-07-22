package tfm.slicing;

import tfm.arcs.Arc;
import tfm.graphs.exceptionsensitive.ESSDG;
import tfm.nodes.GraphNode;

public class PseudoPredicateSlicingAlgorithm extends ClassicSlicingAlgorithm {
    protected GraphNode<?> slicingCriterion;

    public PseudoPredicateSlicingAlgorithm(ESSDG graph) {
        super(graph);
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

    protected boolean ignorePseudoPredicate(Arc arc) {
        GraphNode<?> target = graph.getEdgeTarget(arc);
        return ((ESSDG) graph).isPseudoPredicate(target)
                && arc.isControlDependencyArc()
                && target != slicingCriterion;
    }
}
