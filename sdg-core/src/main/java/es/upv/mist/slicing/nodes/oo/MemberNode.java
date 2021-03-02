package es.upv.mist.slicing.nodes.oo;

import com.github.javaparser.ast.Node;
import es.upv.mist.slicing.nodes.SyntheticNode;

import java.util.LinkedList;

public class MemberNode extends SyntheticNode<Node> {
    protected final MemberNode parent;

    public MemberNode(String instruction, MemberNode parent) {
        super(instruction, null, new LinkedList<>());
        this.parent = parent;
    }

    public MemberNode getParent() {
        return parent;
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
