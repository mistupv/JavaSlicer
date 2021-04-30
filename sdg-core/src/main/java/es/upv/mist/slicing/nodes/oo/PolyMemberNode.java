package es.upv.mist.slicing.nodes.oo;

import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.nodes.GraphNode;

/** A node in an object tree that represents a type in a polymorphic object. */
public class PolyMemberNode extends MemberNode {
    /** Create a new polymorphic member node based on the given type and with the given parent. */
    public PolyMemberNode(ResolvedType type, GraphNode<?> parent) {
        this(type.describe(), parent);
    }

    /** Internal constructor for cloning purposes.
     *  @see #copyToParent(GraphNode) */
    private PolyMemberNode(String label, GraphNode<?> parent) {
        super(label, parent);
    }

    @Override
    public MemberNode copyToParent(GraphNode<?> parent) {
        return new PolyMemberNode(label, parent);
    }
}
