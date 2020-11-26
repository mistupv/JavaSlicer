package es.upv.mist.slicing.arcs.sdg;

import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.Map;

/** An interprocedural arc connecting {@link ActualIONode actual} and {@link FormalIONode formal}
 * nodes. The source and target must match: both must either be inputs or outputs. This arc may be an input or output. */
public class ParameterInOutArc extends InterproceduralArc {
    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        return map;
    }

    @Override
    public boolean isInterproceduralInputArc() {
        GraphNode<?> source = (GraphNode<?>) getSource();
        GraphNode<?> target = (GraphNode<?>) getTarget();
        return source instanceof ActualIONode && ((ActualIONode) source).isInput() &&
                target instanceof FormalIONode && ((FormalIONode) target).isInput();
    }

    @Override
    public boolean isInterproceduralOutputArc() {
        GraphNode<?> source = (GraphNode<?>) getSource();
        GraphNode<?> target = (GraphNode<?>) getTarget();
        return (source instanceof FormalIONode && ((FormalIONode) source).isOutput() &&
                target instanceof ActualIONode && ((ActualIONode) target).isOutput()) ||
                (source instanceof OutputNode && target instanceof CallNode.Return);
    }
}
