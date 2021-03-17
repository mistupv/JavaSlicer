package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

/** Represents a dependence where the source completely defines
 *  the target. This is used when the type of an object or just
 *  its existence is required, but not any specific field. */
public class TotalDefinitionDependenceArc extends Arc {
    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("color", DefaultAttribute.createAttribute("pink"));
        return map;
    }
}
