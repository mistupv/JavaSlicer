package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.graphs.BackwardDataFlowAnalysis;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ExitNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;

import java.util.*;
import java.util.stream.Stream;

/**
 * Base class for generating and placing in an SDG the summary arcs.
 * @param <ActualIn> The type of node for actual-in nodes.
 * @param <FormalOut> The type of node for formal-out nodes.
 * @param <FormalIn> The type of node for formal-in nodes.
 */
public abstract class AbstractSummaryArcAnalyzer<ActualIn extends SyntheticNode<?>, FormalOut extends SyntheticNode<?>, FormalIn extends SyntheticNode<?>>
        extends BackwardDataFlowAnalysis<CallGraph.Vertex, CallGraph.Edge<?>, Map<FormalOut, Set<FormalIn>>> {
    protected final SDG sdg;
    
    protected AbstractSummaryArcAnalyzer(SDG sdg, CallGraph graph) {
        super(graph);
        this.sdg = sdg;
    }

    @Override
    protected Map<FormalOut, Set<FormalIn>> compute(CallGraph.Vertex vertex, Set<CallGraph.Vertex> predecessors) {
        saveDeclaration(vertex);
        return initialValue(vertex);
    }

    @Override
    protected Map<FormalOut, Set<FormalIn>> initialValue(CallGraph.Vertex vertex) {
        Map<FormalOut, Set<FormalIn>> value;
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
    protected Set<FormalOut> getFormalOutNodes(CallableDeclaration<?> declaration) {
        Set<FormalOut> set = new HashSet<>();
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
                .forEach(e -> set.add((FormalOut) e));
        return set;
    }

    /** Given an output or formal-out node, locate the formal-in nodes it depends on.
     *  This search should be performed intra-procedurally, the parent class will take
     *  care of the rest of cases by adding summary arcs computed for other declarations. */
    protected abstract Set<FormalIn> computeFormalIn(FormalOut formalOut);

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
    protected abstract Optional<? extends ActualIn> findActualIn(CallGraph.Edge<?> edge, FormalIn formalIn);

    /** Find the actual-out, return or exception/normal return node that represents the given
     *  formal-out, output or exception/normal exit node in the given call. There may not be one.
     *  In that case, the dependency between formal-in/out should not result in a summary arc. */
    protected abstract Optional<? extends SyntheticNode<?>> findOutputNode(CallGraph.Edge<?> edge, FormalOut formalOut);
}
