package tfm.nodes.factories;

import tfm.nodes.AbstractTypeNodeFactory;
import tfm.nodes.type.NodeType;

public class MethodNodeFactory extends AbstractTypeNodeFactory {
    @Override
    protected NodeType getSpecificType() {
        return NodeType.METHOD;
    }
}
