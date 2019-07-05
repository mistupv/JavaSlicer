package tfm.graphs;

import com.github.javaparser.ast.stmt.Statement;
import tfm.nodes.SDGNode;

public class SDGGraph extends Graph<SDGNode> {

    @Override
    public SDGNode addNode(String instruction, Statement statement) {
        return null;
    }

    @Override
    public String toGraphvizRepresentation() {
        return null;
    }
}
