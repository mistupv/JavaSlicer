package tfm.arcs.pdg;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;
import tfm.arcs.Arc;

import java.util.Map;

/**
 * An arc used in the {@link tfm.graphs.PDG} and {@link tfm.graphs.SDG},
 * representing the declaration of some data linked to its usage (of that value).
 * There is data dependency between two nodes if and only if (1) the source <it>may</it>
 * declare a variable, (2) the destination <it>may</it> use it, and (3) there is a
 * path between the nodes where the variable is not redefined.
 */
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

