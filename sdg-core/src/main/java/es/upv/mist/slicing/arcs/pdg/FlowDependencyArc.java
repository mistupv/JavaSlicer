package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.arcs.Arc;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

/** Represents a data dependency in an object-oriented SDG or PDG. */
public class FlowDependencyArc extends Arc {
    public FlowDependencyArc() {
        super();
    }

    public FlowDependencyArc(String variable) {
        super(variable);
    }

    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("color", DefaultAttribute.createAttribute("red"));
        return map;
    }
}
