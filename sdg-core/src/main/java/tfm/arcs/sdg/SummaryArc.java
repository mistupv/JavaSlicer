package tfm.arcs.sdg;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;

import java.util.Map;

<<<<<<< HEAD:sdg-core/src/main/java/tfm/arcs/sdg/SummaryArc.java
public class SummaryArc extends Arc {
=======
public class ReturnArc extends InterproceduralArc {
>>>>>>> 303de98... Created the SDG's ClassicSlicingAlgorithm:sdg-core/src/main/java/tfm/arcs/sdg/ReturnArc.java
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
        return true;
    }
}
