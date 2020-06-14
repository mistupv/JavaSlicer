package tfm.arcs.sdg;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;
import tfm.arcs.Arc;

import java.util.Map;

public class SummaryArc extends Arc {
    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("bold"));
        return map;
    }

    @Override
    public boolean isInterproceduralInputArc() {
        return false;
    }

    @Override
    public boolean isInterproceduralOutputArc() {
        return false;
    }
}
