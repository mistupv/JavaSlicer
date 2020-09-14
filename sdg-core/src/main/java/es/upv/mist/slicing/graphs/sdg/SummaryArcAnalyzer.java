package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.graphs.BackwardDataFlowAnalysis;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;

import java.util.*;
import java.util.stream.Collectors;

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
        var value = vertexDataMap.get(vertex);
        if (value == null) {
            value = new HashMap<>();
            for (var formalOut : getFormalOutNodes(vertex.getDeclaration()))
                value.put(formalOut, new HashSet<>());
        }
        value.replaceAll((key, oldValue) -> computeFormalIn(key));
        return value;
    }

    protected Set<SyntheticNode<CallableDeclaration<?>>> getFormalOutNodes(CallableDeclaration<?> declaration) {
        Set<SyntheticNode<CallableDeclaration<?>>> set = sdg.vertexSet().stream()
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(FormalIONode::isOutput)
                .collect(Collectors.toSet());
        sdg.vertexSet().stream()
                .filter(OutputNode.class::isInstance)
                .map(OutputNode.class::cast)
                .filter(on -> on.getAstNode().equals(declaration))
                .forEach(set::add);
        return set;
    }

    protected Set<FormalIONode> computeFormalIn(SyntheticNode<CallableDeclaration<?>> formalOut) {
        return sdg.createSlicingAlgorithm().traverseProcedure(formalOut).getGraphNodes().stream()
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(FormalIONode::isInput)
                .collect(Collectors.toSet());
    }

    protected void saveDeclaration(CallGraph.Vertex vertex) {
        var result = vertexDataMap.get(vertex);
        for (CallGraph.Edge<?> edge : graph.incomingEdgesOf(vertex)) {
            for (var entry : result.entrySet()) {
                var actualOutOpt = getActualOut(edge, entry.getKey());
                if (actualOutOpt.isEmpty())
                    continue;
                for (var formalIn : entry.getValue()) {
                    var actualInOpt = getActualIn(edge, formalIn);
                    if (actualInOpt.isEmpty())
                        continue;
                    if (!sdg.containsEdge(actualInOpt.get(), actualOutOpt.get()))
                        sdg.addSummaryArc(actualInOpt.get(), actualOutOpt.get());
                }
            }
        }
    }

    protected Optional<ActualIONode> getActualIn(CallGraph.Edge<?> edge, FormalIONode formalIn) {
        return sdg.vertexSet().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(n -> n.matchesFormalIO(formalIn))
                .findAny();
    }

    protected Optional<SyntheticNode<?>> getActualOut(CallGraph.Edge<?> edge, SyntheticNode<CallableDeclaration<?>> formalOut) {
        if (formalOut instanceof FormalIONode)
            return Optional.ofNullable(getActualOut(edge, (FormalIONode) formalOut));
        if (formalOut instanceof OutputNode)
            return Optional.ofNullable(getActualOut(edge));
        throw new IllegalArgumentException("invalid type");
    }

    protected ActualIONode getActualOut(CallGraph.Edge<?> edge, FormalIONode formalOut) {
        return sdg.vertexSet().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .filter(n -> n.matchesFormalIO(formalOut))
                .findAny().orElse(null);
    }

    protected CallNode.Return getActualOut(CallGraph.Edge<?> edge) {
        return sdg.vertexSet().stream()
                .filter(CallNode.Return.class::isInstance)
                .map(CallNode.Return.class::cast)
                .filter(n -> n.getAstNode() == edge.getCall())
                .findAny().orElse(null);
    }
}
