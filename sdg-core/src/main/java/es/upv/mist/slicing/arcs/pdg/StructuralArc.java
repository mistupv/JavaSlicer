package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

/** Replaces control dependencies that were used to generate a tree-like
 *  structure in the PDG and SDG. Some examples include the connection
 *  of actual/formal-in/out and object trees. They should always be traversed. */
public class StructuralArc extends Arc {
    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        return map;
    }
}
