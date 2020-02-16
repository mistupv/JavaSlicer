package tfm.graphs.augmented;

import tfm.arcs.Arc;
import tfm.nodes.GraphNode;
import tfm.slicing.Slice;
import tfm.utils.ASTUtils;

public class PPDG extends APDG {
    public PPDG() {
        this(new ACFG());
    }

    public PPDG(ACFG acfg) {
        super(acfg);
    }

    @Override
    protected void getSliceNodes(Slice slice, GraphNode<?> node) {
        slice.add(node);

        for (Arc arc : incomingEdgesOf(node)) {
            GraphNode<?> from = getEdgeSource(arc);
            if (slice.contains(from))
                continue;
            getSliceNodesPPDG(slice, from);
        }
    }

    protected void getSliceNodesPPDG(Slice slice, GraphNode<?> node) {
        slice.add(node);
        if (ASTUtils.isPseudoPredicate(node.getAstNode()))
            return;

        for (Arc arc : incomingEdgesOf(node)) {
            GraphNode<?> from = getEdgeSource(arc);
            if (slice.contains(from))
                continue;
            getSliceNodesPPDG(slice, from);
        }
    }
}
