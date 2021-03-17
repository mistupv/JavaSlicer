package es.upv.mist.slicing.nodes.oo;

import com.github.javaparser.ast.Node;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;

import java.util.LinkedList;

/** A synthetic node that represents an object or field that is within a
 *  VariableAction. They are placed in the graph when the PDG is built,
 *  and allow for a more granular representation and slicing of objects. */
public class MemberNode extends SyntheticNode<Node> {
    protected GraphNode<?> parent;

    public MemberNode(String instruction, GraphNode<?> parent) {
        super(instruction, null, new LinkedList<>());
        this.parent = parent;
    }

    public GraphNode<?> getParent() {
        return parent;
    }

    public void setParent(GraphNode<?> parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return String.format("%s{id: %s, label: '%s'}",
                getClass().getSimpleName(),
                getId(),
                getLabel()
        );
    }
}
