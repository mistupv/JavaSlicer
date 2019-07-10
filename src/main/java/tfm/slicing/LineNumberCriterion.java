package tfm.slicing;

import com.github.javaparser.ast.stmt.Statement;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.nodes.SDGNode;
import tfm.utils.Logger;

import java.util.HashSet;
import java.util.Optional;

public class LineNumberCriterion extends SlicingCriterion {

    private int lineNumber;

    public LineNumberCriterion(int lineNumber, String variable) {
        super(variable);

        this.lineNumber = lineNumber;
    }

    @Override
    public Optional<CFGNode> findNode(CFGGraph graph) {
        return Optional.empty();
    }

    @Override
    public Optional<PDGNode> findNode(PDGGraph graph) {
        PDGNode sliceNode = null;

        // find node by line number
        for (PDGNode node : graph.getNodes()) {
            Statement statement = node.getAstNode();

            if (!statement.getBegin().isPresent() || !statement.getEnd().isPresent())
                continue;

            int begin = statement.getBegin().get().line;
            int end = statement.getEnd().get().line;

            Logger.format("begin %s end %s", begin, end);

            if (lineNumber == begin || lineNumber == end) {
                sliceNode = node;
                break;
            }
        }

        return Optional.ofNullable(sliceNode);
    }

    @Override
    public Optional<SDGNode> findNode(SDGGraph graph) {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", lineNumber, variable);
    }
}
