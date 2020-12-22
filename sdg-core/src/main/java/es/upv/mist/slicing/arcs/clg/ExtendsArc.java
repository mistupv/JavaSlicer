package es.upv.mist.slicing.arcs.clg;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.ClassGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;


import java.util.Map;


/**
 * An edge of the {@link ClassGraph}. Represents the inheritance relationship in Java.
 * It goes from the base class to the derived class
 */

public class ExtendsArc extends Arc {
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        return map;
    }
}
