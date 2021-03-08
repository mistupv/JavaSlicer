package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

public class ObjectFlowDependencyArc extends Arc {
    public ObjectFlowDependencyArc() {
        super();
    }

    @Override
    public boolean isObjectFlow() {
        return true;
    }

    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("color", DefaultAttribute.createAttribute("red"));
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        return map;
    }
}
