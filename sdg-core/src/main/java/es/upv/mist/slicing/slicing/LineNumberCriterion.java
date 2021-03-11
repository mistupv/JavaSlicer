package es.upv.mist.slicing.slicing;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** A criterion that locates nodes by line. It may only be used in single-declaration graphs. */
public class LineNumberCriterion implements SlicingCriterion {
    protected static final Position DEFAULT_POSITION = new Position(0, 0);

    protected final int lineNumber;
    protected final String variable;

    public LineNumberCriterion(int lineNumber, String variable) {
        this.variable = variable;
        this.lineNumber = lineNumber;
    }

    @Override
    public Set<GraphNode<?>> findNode(SDG graph) {
        Optional<CompilationUnit> optCu = findCompilationUnit(graph.getCompilationUnits());
        if (optCu.isEmpty())
            throw new NoSuchElementException();
        return optCu.get().findAll(Node.class, this::matchesLine).stream()
                .map(graph::findNodeByASTNode)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    /** Locates the compilation unit that corresponds to this criterion's file. */
    protected Optional<CompilationUnit> findCompilationUnit(NodeList<CompilationUnit> cus) {
        return cus.getFirst();
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
