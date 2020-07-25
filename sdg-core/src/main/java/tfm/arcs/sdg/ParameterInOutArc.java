package tfm.arcs.sdg;

import org.jgrapht.io.Attribute;
import org.jgrapht.io.DefaultAttribute;
import tfm.nodes.GraphNode;
import tfm.nodes.type.NodeType;

import java.util.Map;

public class ParameterInOutArc extends InterproceduralArc {
    @Override
    public Map<String, Attribute> getDotAttributes() {
        Map<String, Attribute> map = super.getDotAttributes();
        map.put("style", DefaultAttribute.createAttribute("dashed"));
        return map;
    }

    @Override
    public boolean isInterproceduralInputArc() {
        return ((GraphNode<?>) getSource()).getNodeType() == NodeType.ACTUAL_IN &&
                ((GraphNode<?>) getTarget()).getNodeType() == NodeType.FORMAL_IN;
    }

    @Override
    public boolean isInterproceduralOutputArc() {
        return (((GraphNode<?>) getSource()).getNodeType() == NodeType.FORMAL_OUT &&
                ((GraphNode<?>) getTarget()).getNodeType() == NodeType.ACTUAL_OUT) ||
                (((GraphNode<?>) getSource()).getNodeType() == NodeType.METHOD_OUTPUT &&
                ((GraphNode<?>) getTarget()).getNodeType() == NodeType.METHOD_CALL_RETURN);
    }
}
