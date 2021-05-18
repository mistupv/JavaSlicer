package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ExitNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ReturnNode;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates the summary arcs between actual-in and actual-out, return and exception/exit return nodes.
 */
public class SummaryArcAnalyzer extends AbstractSummaryArcAnalyzer<ActualIONode, SyntheticNode<CallableDeclaration<?>>, FormalIONode> {
    public SummaryArcAnalyzer(SDG sdg, CallGraph graph) {
        super(sdg, graph);
    }

    @Override
    protected Set<FormalIONode> computeFormalIn(SyntheticNode<CallableDeclaration<?>> formalOut) {
        return sdg.createSlicingAlgorithm().traverseProcedure(formalOut).getGraphNodes().stream()
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(FormalIONode::isInput)
                .collect(Collectors.toSet());
    }

    @Override
    protected Collection<ActualIONode> findActualIn(CallGraph.Edge<?> edge, FormalIONode formalIn) {
        return sdg.vertexSet().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(n -> n.matchesFormalIO(formalIn))
                .collect(Collectors.toSet());
    }

    @Override
    protected Collection<? extends SyntheticNode<?>> findOutputNode(CallGraph.Edge<?> edge, SyntheticNode<CallableDeclaration<?>> formalOut) {
        if (formalOut instanceof FormalIONode)
            return findActualOut(edge, (FormalIONode) formalOut);
        if (formalOut instanceof OutputNode)
            return Set.of(findReturnNode(edge));
        if (formalOut instanceof ExitNode)
            return getReturnNode(edge, (ExitNode) formalOut);
        throw new IllegalArgumentException("invalid type");
    }

    /** Find the actual-out node that corresponds to the given formal-out in the given call.
     *  To locate any actual-out, you should use {@link #findOutputNode(CallGraph.Edge, SyntheticNode)}. */
    protected Collection<ActualIONode> findActualOut(CallGraph.Edge<?> edge, FormalIONode formalOut) {
        return sdg.vertexSet().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(n -> n.matchesFormalIO(formalOut))
                .collect(Collectors.toSet());
    }

    /** Find the return node of the given call. There is only one per method.
     *  To locate any actual-out, you should use {@link #findOutputNode(CallGraph.Edge, SyntheticNode)}. */
    protected CallNode.Return findReturnNode(CallGraph.Edge<?> edge) {
        return sdg.vertexSet().stream()
                .filter(CallNode.Return.class::isInstance)
                .map(CallNode.Return.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .findAny().orElseThrow();
    }

    /** Find the exception/normal return node that corresponds to the given exception/normal exit in the given call.
     *  To locate any actual-out, you should use {@link #findOutputNode(CallGraph.Edge, SyntheticNode)}. */
    protected Collection<ReturnNode> getReturnNode(CallGraph.Edge<?> edge, ExitNode exitNode) {
        return sdg.vertexSet().stream()
                .filter(ReturnNode.class::isInstance)
                .map(ReturnNode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(exitNode::matchesReturnNode)
                .collect(Collectors.toSet());
    }
}
