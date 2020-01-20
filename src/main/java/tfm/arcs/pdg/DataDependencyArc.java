package tfm.arcs.pdg;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;
import tfm.arcs.Arc;

import java.util.Map;

public class DataDependencyArc extends Arc {
    private final String variable;

    public DataDependencyArc(String variable) {
        super();
        this.variable = variable;
    }

    @Override
    public String getLabel() {
        return variable;
    }

    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        map.put("color", DefaultAttribute.createAttribute("red"));
        return map;
    }
}

