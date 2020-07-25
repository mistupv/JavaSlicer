package tfm.graphs.augmented;

import tfm.nodes.GraphNode;
import tfm.slicing.PseudoPredicateSlicingAlgorithm;
import tfm.slicing.Slice;
import tfm.slicing.SlicingCriterion;
import tfm.utils.NodeNotFoundException;

import java.util.Optional;

public class PPDG extends APDG {
    public PPDG() {
        this(new ACFG());
    }

    public PPDG(ACFG acfg) {
        super(acfg);
    }

    @Override
    public Slice slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> node = slicingCriterion.findNode(this);
        if (node.isEmpty())
            throw new NodeNotFoundException(slicingCriterion);
        return new PseudoPredicateSlicingAlgorithm(this).traverse(node.get());
    }
}
