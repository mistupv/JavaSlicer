package tfm.arcs.pdg;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;
import tfm.arcs.Arc;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
import tfm.nodes.VariableAction;

import java.util.Map;
import java.util.Objects;

/**
 * An arc used in the {@link PDG} and {@link SDG},
 * representing the declaration of some data linked to its usage (of that value).
 * There is data dependency between two nodes if and only if (1) the source <it>may</it>
 * declare a variable, (2) the destination <it>may</it> use it, and (3) there is a
 * path between the nodes where the variable is not redefined.
 */
public class DataDependencyArc extends Arc {
    protected final VariableAction source;
    protected final VariableAction target;

    public DataDependencyArc(VariableAction.Definition source, VariableAction.Usage target) {
        super(source.getVariable());
        this.source = source;
        this.target = target;
    }

    public DataDependencyArc(VariableAction.Declaration source, VariableAction.Definition target) {
        super(source.getVariable());
        this.source = source;
        this.target = target;
    }

    public VariableAction getSource() {
        return source;
    }

    public VariableAction getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
                && o instanceof DataDependencyArc
                && Objects.equals(source, ((DataDependencyArc) o).source)
                && Objects.equals(target, ((DataDependencyArc) o).target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), source, target);
    }

    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("color", DefaultAttribute.createAttribute(target.isDefinition() ? "pink" : "red"));
        return map;
    }
}

