package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.BackwardDataFlowAnalysis;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.exceptionsensitive.ExitNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;
import es.upv.mist.slicing.nodes.oo.MemberNode;

import java.util.*;
import java.util.stream.Stream;

public class SummaryArcAnalyzer extends BackwardDataFlowAnalysis<CallGraph.Vertex, CallGraph.Edge<?>, Map<SyntheticNode<?>, Set<SyntheticNode<?>>>> {
    protected final JSysDG sdg;

    public SummaryArcAnalyzer(JSysDG sdg, CallGraph graph) {
        super(graph);
        this.sdg = sdg;
    }

    @Override
    protected Map<SyntheticNode<?>, Set<SyntheticNode<?>>> compute(CallGraph.Vertex vertex, Set<CallGraph.Vertex> predecessors) {
        saveDeclaration(vertex);
        return initialValue(vertex);
    }

    @Override
    protected Map<SyntheticNode<?>, Set<SyntheticNode<?>>> initialValue(CallGraph.Vertex vertex) {
        Map<SyntheticNode<?>, Set<SyntheticNode<?>>> value;
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

    protected Set<SyntheticNode<?>> getFormalOutNodes(CallableDeclaration<?> declaration) {
        Set<SyntheticNode<?>> set = new HashSet<>();
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
        for (var node : Set.copyOf(set)) {
            if (node.getVariableActions().isEmpty())
                continue;
            assert node.getVariableActions().size() == 1;
            VariableAction action = node.getVariableActions().get(0);
            if (action.hasObjectTree()) {
                set.add(action.getObjectTree().getMemberNode());
                for (MemberNode memberNode : action.getObjectTree().nodeIterable())
                    set.add(memberNode);
            }
        }
        return set;
    }

    protected Set<SyntheticNode<?>> computeFormalIn(SyntheticNode<?> formalOut) {
        Set<SyntheticNode<?>> result = new HashSet<>();
        for (GraphNode<?> graphNode : sdg.createSlicingAlgorithm().traverseProcedure(formalOut).getGraphNodes())
            if (isFormalIn(graphNode) && graphNode instanceof SyntheticNode)
                result.add((SyntheticNode<?>) graphNode);
        return result;
    }

    protected Optional<? extends SyntheticNode<?>> findActualIn(CallGraph.Edge<?> edge, SyntheticNode<?> formalIn) {
        return sdg.incomingEdgesOf(formalIn).stream()
                .filter(Arc::isInterproceduralInputArc)
                .map(sdg::getEdgeSource)
                .filter(actualIn -> goToParent(actualIn).getAstNode() == edge.getCall())
                .map(node -> (SyntheticNode<?>) node)
                .findAny();
    }

    protected Optional<? extends SyntheticNode<?>> findOutputNode(CallGraph.Edge<?> edge, SyntheticNode<?> formalOut) {
        return sdg.outgoingEdgesOf(formalOut).stream()
                .filter(Arc::isInterproceduralOutputArc)
                .map(sdg::getEdgeTarget)
                .filter(actualOut -> goToParent(actualOut).getAstNode() == edge.getCall())
                .map(node -> (SyntheticNode<?>) node)
                .findAny();
    }

    private boolean isFormalIn(GraphNode<?> graphNode) {
        GraphNode<?> parent = goToParent(graphNode);
        return parent instanceof FormalIONode && ((FormalIONode) parent).isInput();
    }

    private GraphNode<?> goToParent(GraphNode<?> memberNode) {
        if (memberNode instanceof MemberNode)
            return goToParent(((MemberNode) memberNode).getParent());
        return memberNode;
    }
}
