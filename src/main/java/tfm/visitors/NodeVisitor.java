package tfm.visitors;

import edg.graphlib.Graph;
import edg.graphlib.Vertex;
import edg.graphlib.Visitor;
import tfm.arcs.data.ArcData;
import tfm.nodes.Node;

public abstract class NodeVisitor<NodeType extends Node> implements Visitor<String, ArcData> {

    @Override
    public void visit(Graph<String, ArcData> g, Vertex<String, ArcData> v) {
        this.visit((NodeType) v);
    }

    public abstract void visit(NodeType node);
}
