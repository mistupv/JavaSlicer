package tfm.graphs;

import com.github.javaparser.ast.stmt.Statement;
import tfm.nodes.SDGNode;
import tfm.slicing.SlicingCriterion;

import java.util.Set;

public class SDGGraph extends Graph<SDGNode> {

    @Override
    public SDGNode addNode(String instruction, Statement statement) {
        return null;
    }

    @Override
    public String toGraphvizRepresentation() {
        return null;
    }

    @Override
    public Graph<SDGNode> slice(SlicingCriterion slicingCriterion) {
        return this;
    }
}
