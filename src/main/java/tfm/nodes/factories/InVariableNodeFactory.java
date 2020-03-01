package tfm.nodes.factories;

import tfm.nodes.AbstractTypeNodeFactory;
import tfm.nodes.type.NodeType;

public class InVariableNodeFactory extends AbstractTypeNodeFactory {
    @Override
    protected NodeType getSpecificType() {
        return NodeType.VARIABLE_IN;
    }
}
