package tfm.slicing;

import com.github.javaparser.ast.stmt.Statement;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.nodes.SDGNode;
import tfm.utils.Logger;

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
        // find node by line number
        return graph.getNodes().stream().filter(node -> {
            Statement statement = node.getAstNode();

            if (!statement.getBegin().isPresent() || !statement.getEnd().isPresent())
                return false;

            int begin = statement.getBegin().get().line;
            int end = statement.getEnd().get().line;

            Logger.format("begin %s end %s", begin, end);

            return lineNumber == begin || lineNumber == end;
        }).findFirst();
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
