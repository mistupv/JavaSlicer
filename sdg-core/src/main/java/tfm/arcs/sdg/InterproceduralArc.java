package tfm.arcs.sdg;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;
import tfm.arcs.Arc;

import java.util.Map;

public abstract class InterproceduralArc extends Arc {
    protected InterproceduralArc() {
        super();
    }

    @Override
    public Map<String, Attribute> getDotAttributes() {
        var map = super.getDotAttributes();
        map.put("penwidth", DefaultAttribute.createAttribute("3"));
        if (isInterproceduralInputArc())
            map.put("color", DefaultAttribute.createAttribute("orange"));
        else if (isInterproceduralOutputArc())
            map.put("color", DefaultAttribute.createAttribute("blue"));
        else
            map.put("color", DefaultAttribute.createAttribute("green"));
        return map;
    }

    @Override
    public abstract boolean isInterproceduralInputArc();

    @Override
    public abstract boolean isInterproceduralOutputArc();
}
