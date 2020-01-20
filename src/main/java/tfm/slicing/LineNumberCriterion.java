package tfm.slicing;

import com.github.javaparser.ast.Node;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;

import java.util.Optional;

public class LineNumberCriterion extends SlicingCriterion {

    private int lineNumber;

    public LineNumberCriterion(int lineNumber, String variable) {
        super(variable);

        this.lineNumber = lineNumber;
    }

    @Override
    public Optional<GraphNode<?>> findNode(CFGGraph graph) {
        return Optional.empty();
    }

    @Override
    public Optional<GraphNode<?>> findNode(PDGGraph graph) {
        // find node by line number
        return graph.vertexSet().stream().filter(node -> {
            Node astNode = node.getAstNode();

            if (!astNode.getBegin().isPresent() || !astNode.getEnd().isPresent())
                return false;

            int begin = astNode.getBegin().get().line;
            int end = astNode.getEnd().get().line;

            Logger.format("begin %s end %s", begin, end);

            return lineNumber == begin || lineNumber == end;
        }).findFirst();
    }

    @Override
    public Optional<GraphNode<?>> findNode(SDGGraph graph) {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", lineNumber, variable);
    }
}
