package tfm.utils;

import tfm.slicing.SlicingCriterion;

public class NodeNotFoundException extends RuntimeException {

    public NodeNotFoundException(SlicingCriterion slicingCriterion) {
        super("Node not found for slicing criterion: " + slicingCriterion);
    }

    public NodeNotFoundException(String message) {
        super(message);
    }
}
