package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

public class TotalDefinitionDependenceArc extends Arc {
    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("color", DefaultAttribute.createAttribute("pink"));
        return map;
    }
}
