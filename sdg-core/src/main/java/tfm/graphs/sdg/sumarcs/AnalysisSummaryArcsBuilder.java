package tfm.graphs.sdg.sumarcs;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.arcs.Arc;
import tfm.arcs.sdg.CallArc;
import tfm.graphs.CallGraph;
import tfm.graphs.sdg.SDG;
import tfm.nodes.ActualIONode;
import tfm.nodes.FormalIONode;
import tfm.nodes.GraphNode;
import tfm.nodes.type.NodeType;
import tfm.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Performs a fixed point analysis over the call graph of a given SDG
 */
public class AnalysisSummaryArcsBuilder extends SummaryArcsBuilder {

    private static class SummaryArcPair {
        FormalIONode in;
        GraphNode<?> out; // out node is either FormalIONode or METHOD_OUTPUT

        SummaryArcPair(FormalIONode in, GraphNode<?> out) {
            this.in = in; this.out = out;
        }
    }

    private final SDG sdg;
    private final CallGraph callGraph;

    protected Map<CallableDeclaration<?>, Set<SummaryArcPair>> vertexDataMap = new HashMap<>();
    protected boolean built = false;

    public AnalysisSummaryArcsBuilder(SDG sdg) {
        super(sdg);

        CallGraph callGraph = new CallGraph();
        callGraph.build(sdg.getCompilationUnits());

        this.sdg = sdg;
        this.callGraph = callGraph;
    }

    public AnalysisSummaryArcsBuilder(SDG sdg, CallGraph callGraph) {
        super(sdg);

        this.sdg = sdg;
        this.callGraph = callGraph;
    }

    public Set<SummaryArcPair> getResult(MethodDeclaration vertex) {
        return vertexDataMap.get(vertex);
    }

    @Override
    public void visit() {
        assert !built;
        List<CallableDeclaration<?>> workList = new LinkedList<>(callGraph.vertexSet());
        callGraph.vertexSet().forEach(v -> vertexDataMap.put(v, computeSummaryArcs(v)));
        while (!workList.isEmpty()) {
            List<CallableDeclaration<?>> newWorkList = new LinkedList<>();
            for (CallableDeclaration<?> vertex : workList) {
                updateVertex(vertex);
                Set<SummaryArcPair> newValue = computeSummaryArcs(vertex); // now with new arcs!!!
                if (!Objects.equals(vertexDataMap.get(vertex), newValue)) {
                    vertexDataMap.put(vertex, newValue);
                    newWorkList.addAll(callGraph.incomingEdgesOf(vertex).stream()
                            .map(callGraph::getEdgeSource).collect(Collectors.toSet()));
                }
            }
            workList = newWorkList;
        }
        vertexDataMap = Collections.unmodifiableMap(vertexDataMap);
        built = true;
    }

    protected void updateVertex(CallableDeclaration<?> declaration) {
        if (!declaration.isMethodDeclaration()) {
            return; // Parse only method declarations
        }

        Optional<GraphNode<MethodDeclaration>> optionalMethodDeclarationNode = sdg.findNodeByASTNode(declaration.asMethodDeclaration());

        if (optionalMethodDeclarationNode.isEmpty()) {
            return;
        }

        GraphNode<MethodDeclaration> methodDeclarationNode = optionalMethodDeclarationNode.get();

        // Get call arcs from this declaration
        Set<CallArc> methodCallExprNodes = sdg.incomingEdgesOf(methodDeclarationNode).stream()
                .filter(Arc::isCallArc)
                .map(Arc::asCallArc)
                .collect(Collectors.toSet());

        for (CallArc callArc : methodCallExprNodes) {
            GraphNode<?> methodCallNode = sdg.getEdgeSource(callArc);

            for (SummaryArcPair summaryArcPair : vertexDataMap.getOrDefault(declaration, Utils.emptySet())) {
                FormalIONode inFormalNode = summaryArcPair.in;
                GraphNode<?> outFormalNode = summaryArcPair.out;

                Optional<ActualIONode> optionalIn = sdg.outgoingEdgesOf(methodCallNode).stream()
                        .filter(arc -> (sdg.getEdgeTarget(arc)).getNodeType().is(NodeType.ACTUAL_IN))
                        .map(arc -> (ActualIONode) sdg.getEdgeTarget(arc))
                        .filter(actualIONode -> actualIONode.matchesFormalIO(inFormalNode))
                        .findFirst();

                Optional<? extends GraphNode<?>> optionalOut = sdg.outgoingEdgesOf(methodCallNode).stream()
                        .map(sdg::getEdgeTarget)
                        .filter(node -> node.getNodeType().is(NodeType.ACTUAL_OUT))
                        .filter(actualNode -> {
                            if (actualNode instanceof ActualIONode) {
                                return outFormalNode instanceof FormalIONode
                                    && ((ActualIONode) actualNode).matchesFormalIO((FormalIONode) outFormalNode);
                            }
                            // otherwise, actualNode must be METHOD_CALL_RETURN
                            if (actualNode.getNodeType() != NodeType.METHOD_CALL_RETURN) {
                                return false;
                            }

                            return outFormalNode.getNodeType() == NodeType.METHOD_OUTPUT;
                        })
                        .findFirst();

                if (optionalIn.isEmpty() || optionalOut.isEmpty()) {
                    continue;
                }

                sdg.addSummaryArc(optionalIn.get(), optionalOut.get());
            }
        }
    }

    protected Set<SummaryArcPair> computeSummaryArcs(CallableDeclaration<?> declaration) {
        Optional<GraphNode<MethodDeclaration>> optionalMethodDeclarationNode = sdg.findNodeByASTNode(declaration.asMethodDeclaration());

        if (optionalMethodDeclarationNode.isEmpty()) {
            return Utils.emptySet();
        }

        GraphNode<MethodDeclaration> methodDeclarationNode = optionalMethodDeclarationNode.get();

        // Get formal out nodes from declaration
        Set<GraphNode<?>> formalOutNodes = sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                .filter(Arc::isControlDependencyArc)
                .map(sdg::getEdgeTarget)
                .filter(node -> node.getNodeType().is(NodeType.FORMAL_OUT))
                .collect(Collectors.toSet());

        Set<SummaryArcPair> res = new HashSet<>();

        for (GraphNode<?> formalOutNode : formalOutNodes) {
            for (FormalIONode formalInNode : findReachableFormalInNodes(formalOutNode)) {
                res.add(new SummaryArcPair(formalInNode, formalOutNode));
            }
        }

        return res;
    }

    private Set<FormalIONode> findReachableFormalInNodes(GraphNode<?> formalOutNode) {
        return this.doFindReachableFormalInNodes(formalOutNode, Utils.emptySet());
    }

    private Set<FormalIONode> doFindReachableFormalInNodes(GraphNode<?> root, Set<Long> visited) {
        visited.add(root.getId());

        Set<FormalIONode> res = Utils.emptySet();

        if (root.getNodeType().is(NodeType.FORMAL_IN)) {
            res.add((FormalIONode) root);
        } else {
            for (Arc arc : sdg.incomingEdgesOf(root)) {
                GraphNode<?> nextNode = sdg.getEdgeSource(arc);

                if (visited.contains(nextNode.getId())) {
                    continue;
                }

                if (arc.isDataDependencyArc() || arc.isControlDependencyArc() || arc.isSummaryArc()) {
                    res.addAll(this.doFindReachableFormalInNodes(nextNode, visited));
                }
            }
        }

        return res;
    }
}
