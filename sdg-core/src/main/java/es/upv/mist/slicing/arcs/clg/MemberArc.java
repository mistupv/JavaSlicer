package es.upv.mist.slicing.arcs.clg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.ClassGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

/**
 * An edge of the {@link ClassGraph}. It represents the membership of a class node.
 * It links the class node and its inner data members/function definitions.
 */

public class MemberArc extends Arc {
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        return map;
    }
}
