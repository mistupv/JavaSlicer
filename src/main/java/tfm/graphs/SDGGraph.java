package tfm.graphs;

import com.github.javaparser.ast.stmt.Statement;
import tfm.nodes.SDGNode;
import tfm.slicing.SlicingCriterion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SDGGraph extends Graph<SDGNode> {

    private List<PDGGraph> pdgGraphList;

    public SDGGraph() {
        this.pdgGraphList = new ArrayList<>();
    }

    @Override
    public SDGNode addNode(String instruction, Statement statement) {
        return null;
    }

    @Override
    public String toGraphvizRepresentation() {
        return pdgGraphList.stream().map(PDGGraph::toGraphvizRepresentation).collect(Collectors.joining("\n"));
    }

    @Override
    public Graph<SDGNode> slice(SlicingCriterion slicingCriterion) {
        return this;
    }

    public void addPDG(PDGGraph pdgGraph) {
        this.pdgGraphList.add(pdgGraph);
    }
}
