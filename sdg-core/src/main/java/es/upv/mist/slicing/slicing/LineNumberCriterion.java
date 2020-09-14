package es.upv.mist.slicing.slicing;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.utils.Logger;

import java.util.Optional;

/** A criterion that locates nodes by line. It may only be used in single-declaration graphs. */
public class LineNumberCriterion extends SlicingCriterion {
    protected static final Position DEFAULT_POSITION = new Position(0, 0);

    protected final int lineNumber;

    public LineNumberCriterion(int lineNumber, String variable) {
        super(variable);
        this.lineNumber = lineNumber;
    }

    @Override
    public Optional<GraphNode<?>> findNode(CFG graph) {
        return Optional.empty();
    }

    @Override
    public Optional<GraphNode<?>> findNode(PDG graph) {
        // find node by line number
        return graph.vertexSet().stream().filter(node -> {
            Node astNode = node.getAstNode();

            if (astNode.getBegin().isEmpty() || astNode.getEnd().isEmpty())
                return false;

            int begin = astNode.getBegin().get().line;
            int end = astNode.getEnd().get().line;

            Logger.format("begin %s end %s", begin, end);

            return lineNumber == begin || lineNumber == end;
        }).findFirst();
    }

    @Override
    public Optional<GraphNode<?>> findNode(SDG graph) {
        return Optional.empty();
    }

    /** Check if a node matches the criterion's line. */
    protected boolean matchesLine(Node node) {
        return node.getBegin().orElse(DEFAULT_POSITION).line == lineNumber;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", lineNumber, variable);
    }
}
