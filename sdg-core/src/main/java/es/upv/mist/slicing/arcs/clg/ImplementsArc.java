package es.upv.mist.slicing.arcs.clg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.ClassGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

/**
 * An edge of the {@link ClassGraph}. Represents the implements relationship in Java.
 * It goes from the interface to the class that implements it.
 */

public class ImplementsArc extends Arc {
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        return map;
    }
}
