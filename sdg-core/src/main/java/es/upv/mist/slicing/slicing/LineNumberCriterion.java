package es.upv.mist.slicing.slicing;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import es.upv.mist.slicing.arcs.pdg.StructuralArc;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.ObjectTree;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A criterion that locates nodes by line. It may only be used in single-declaration graphs. */
public class LineNumberCriterion implements SlicingCriterion {
    protected static final Position DEFAULT_POSITION = new Position(0, 0);

    protected final int lineNumber;
    protected String variable;

    public LineNumberCriterion(int lineNumber, String variable) {
        this.variable = variable;
        this.lineNumber = lineNumber;
    }

    @Override
    public Set<GraphNode<?>> findNode(SDG graph) {
        Optional<CompilationUnit> optCu = findCompilationUnit(graph.getCompilationUnits());
        if (optCu.isEmpty())
            throw new NoSuchElementException();

        Set<GraphNode<?>> set = optCu.get().findAll(Node.class, this::matchesLine).stream()
                .map(graph::findNodeByASTNode)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(node -> locateVariableNodes(node, graph))
                .collect(Collectors.toSet());
        if (set.isEmpty() && !variable.startsWith("this")) {
            variable = "this." + variable;
            return findNode(graph);
        } else {
            return set;
        }
    }

    /** Locates the compilation unit that corresponds to this criterion's file. */
    protected Optional<CompilationUnit> findCompilationUnit(NodeList<CompilationUnit> cus) {
        return cus.getFirst();
    }

    /** Check if a node matches the criterion's line. */
    protected boolean matchesLine(Node node) {
        return node.getBegin().orElse(DEFAULT_POSITION).line == lineNumber;
    }

    protected Stream<GraphNode<?>> locateVariableNodes(GraphNode<?> graphNode, SDG graph) {
        if (variable == null)
            return locateAllNodes(graphNode, graph);
        return locateAllNodes(graphNode, graph)
                .map(GraphNode::getVariableActions).flatMap(List::stream)
                .flatMap(variableAction -> {
                    if (variableAction.getName().equals(variable)) {
                        if (variableAction.hasObjectTree())
                            return Stream.of(variableAction.getObjectTree().getMemberNode());
                        else
                            return Stream.of(variableAction.getGraphNode());
                    } else if (variable.contains(".") && variableAction.getName().equals(ObjectTree.removeFields(variable))) {
                        if (variableAction.hasPolyTreeMember(variable))
                            return variableAction.getObjectTree().getNodesForPoly(variable).stream();
                        else
                            return Stream.empty();
                    } else {
                        return Stream.empty();
                    }
                });
    }

    protected Stream<GraphNode<?>> locateAllNodes(GraphNode<?> graphNode, SDG graph) {
        Stream<GraphNode<?>> result = graph.outgoingEdgesOf(graphNode).stream()
                .filter(StructuralArc.class::isInstance)
                .map(graph::getEdgeTarget)
                .flatMap(node -> locateAllNodes(node, graph));
        return Stream.concat(Stream.of(graphNode), result);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", lineNumber, variable);
    }
}
