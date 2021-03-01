package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.graphs.BackwardDataFlowAnalysis;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ExitNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ReturnNode;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SummaryArcAnalyzer extends BackwardDataFlowAnalysis<CallGraph.Vertex, CallGraph.Edge<?>, Map<SyntheticNode<CallableDeclaration<?>>, Set<FormalIONode>>> {
    protected final SDG sdg;

    public SummaryArcAnalyzer(SDG sdg, CallGraph graph) {
        super(graph);
        this.sdg = sdg;
    }

    @Override
    protected Map<SyntheticNode<CallableDeclaration<?>>, Set<FormalIONode>> compute(CallGraph.Vertex vertex, Set<CallGraph.Vertex> predecessors) {
        saveDeclaration(vertex);
        return initialValue(vertex);
    }

    @Override
    protected Map<SyntheticNode<CallableDeclaration<?>>, Set<FormalIONode>> initialValue(CallGraph.Vertex vertex) {
        Map<SyntheticNode<CallableDeclaration<?>>, Set<FormalIONode>> value;
        if (vertexDataMap.containsKey(vertex)) {
            value = vertexDataMap.get(vertex);
        } else {
            value = new HashMap<>();
            for (var formalOut : getFormalOutNodes(vertex.getDeclaration()))
                value.put(formalOut, new HashSet<>());
        }
        value.replaceAll((key, oldValue) -> computeFormalIn(key));
        return value;
    }

    /** Obtain all nodes that represent the output of a method declaration. These include formal-out,
     *  return nodes and normal/exception exit nodes (for exception handling). */
    protected Set<SyntheticNode<CallableDeclaration<?>>> getFormalOutNodes(CallableDeclaration<?> declaration) {
        Set<SyntheticNode<CallableDeclaration<?>>> set = new HashSet<>();
        Stream.concat(
                Stream.concat(
                        sdg.vertexSet().stream() // formal-out nodes
                                .filter(FormalIONode.class::isInstance)
                                .map(FormalIONode.class::cast)
                                .filter(FormalIONode::isOutput),
                        sdg.vertexSet().stream() // output nodes (the value returned)
                                .filter(OutputNode.class::isInstance)
                                .map(OutputNode.class::cast)),
                sdg.vertexSet().stream() // normal/exception exit nodes (for exception handling)
                        .filter(ExitNode.class::isInstance)
                        .map(ExitNode.class::cast))
                // Only nodes that match the current declaration
                .filter(node -> node.getAstNode() == declaration)
                .forEach(set::add);
        return set;
    }

    /** Given an output or formal-out node, locate the formal-in nodes it depends on.
     *  This search should be performed intra-procedurally, the parent class will take
     *  care of the rest of cases by adding summary arcs computed for other declarations. */
    protected Set<FormalIONode> computeFormalIn(SyntheticNode<CallableDeclaration<?>> formalOut) {
        return sdg.createSlicingAlgorithm().traverseProcedure(formalOut).getGraphNodes().stream()
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(FormalIONode::isInput)
                .collect(Collectors.toSet());
    }

    /** Generate all summary arcs for a given call. Arc generation should be idempotent:
     *  if this method is called repeatedly it should not create duplicate summary arcs. */
    protected void saveDeclaration(CallGraph.Vertex vertex) {
        var result = vertexDataMap.get(vertex);
        for (CallGraph.Edge<?> edge : graph.incomingEdgesOf(vertex)) {
            for (var entry : result.entrySet()) {
                var actualOutOpt = findOutputNode(edge, entry.getKey());
                if (actualOutOpt.isEmpty())
                    continue;
                for (var formalIn : entry.getValue()) {
                    var actualInOpt = findActualIn(edge, formalIn);
                    if (actualInOpt.isEmpty())
                        continue;
                    if (!sdg.containsEdge(actualInOpt.get(), actualOutOpt.get()))
                        sdg.addSummaryArc(actualInOpt.get(), actualOutOpt.get());
                }
            }
        }
    }

    /** Find the actual-in that represents the given formal-in in the given call.
     *  There may not be one. In that case, the dependency between formal-in/out should
     *  not result in a summary arc. */
    protected Optional<ActualIONode> findActualIn(CallGraph.Edge<?> edge, FormalIONode formalIn) {
        return sdg.vertexSet().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(n -> n.matchesFormalIO(formalIn))
                .findAny();
    }

    /** Find the actual-out, return or exception/normal return node that represents the given
     *  formal-out, output or exception/normal exit node in the given call. There may not be one.
     *  In that case, the dependency between formal-in/out should not result in a summary arc. */
    protected Optional<? extends SyntheticNode<?>> findOutputNode(CallGraph.Edge<?> edge, SyntheticNode<CallableDeclaration<?>> formalOut) {
        if (formalOut instanceof FormalIONode)
            return findActualOut(edge, (FormalIONode) formalOut);
        if (formalOut instanceof OutputNode)
            return Optional.of(findReturnNode(edge));
        if (formalOut instanceof ExitNode)
            return getReturnNode(edge, (ExitNode) formalOut);
        throw new IllegalArgumentException("invalid type");
    }

    /** Find the actual-out node that corresponds to the given formal-out in the given call.
     *  To locate any actual-out, you should use {@link #findOutputNode(CallGraph.Edge, SyntheticNode)}. */
    protected Optional<ActualIONode> findActualOut(CallGraph.Edge<?> edge, FormalIONode formalOut) {
        return sdg.vertexSet().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(n -> n.matchesFormalIO(formalOut))
                .findAny();
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
    protected Optional<ReturnNode> getReturnNode(CallGraph.Edge<?> edge, ExitNode exitNode) {
        return sdg.vertexSet().stream()
                .filter(ReturnNode.class::isInstance)
                .map(ReturnNode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(exitNode::matchesReturnNode)
                .findAny();
    }
}
