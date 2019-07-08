package tfm.slicing;

import tfm.graphs.Graph;
import tfm.nodes.Node;

public interface Slicer<NodeType extends Node<?>, G extends Graph<NodeType>> {

    G slice(String variable, int line);
}
