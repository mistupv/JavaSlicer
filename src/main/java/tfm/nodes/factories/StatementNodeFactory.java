package tfm.nodes.factories;

import tfm.nodes.AbstractTypeNodeFactory;
import tfm.nodes.type.NodeType;

public class StatementNodeFactory extends AbstractTypeNodeFactory {
    @Override
    protected NodeType getSpecificType() {
        return NodeType.STATEMENT;
    }
}
