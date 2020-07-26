package tfm.graphs.sdg.sumarcs;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
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

public abstract class AnalysisSummaryArcsBuilder extends SummaryArcsBuilder {

    private static class SummaryArcPair {
        FormalIONode in;
        FormalIONode out;
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

    // Al acabar, los summary edges estan colocados
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

        // Copiar los summaries que hay en 'vertexDataMap' a todas las llamadas a metodo contenidas en este vertices (ver outgoingEdges)

        // Get call arcs from this declaration
        Set<CallArc> methodCallExprNodes = sdg.incomingEdgesOf(methodDeclarationNode).stream()
                .filter(Arc::isCallArc)
                .map(Arc::asCallArc)
                .collect(Collectors.toSet());

        for (CallArc callArc : methodCallExprNodes) {
            GraphNode<?> methodCallNode = sdg.getEdgeSource(callArc);

            for (SummaryArcPair summaryArcPair : vertexDataMap.getOrDefault(declaration, Utils.emptySet())) {
                FormalIONode inFormalNode = summaryArcPair.in;
                FormalIONode outFormalNode = summaryArcPair.out;

                Optional<ActualIONode> optionalIn = sdg.outgoingEdgesOf(methodCallNode).stream()
                        .filter(arc -> (sdg.getEdgeTarget(arc)).getNodeType() == NodeType.ACTUAL_IN)
                        .map(arc -> (ActualIONode) sdg.getEdgeTarget(arc))
                        .filter(actualIONode -> actualIONode.matchesFormalIO(inFormalNode))
                        .findFirst();

                Optional<ActualIONode> optionalOut = sdg.outgoingEdgesOf(methodCallNode).stream()
                        .filter(arc -> (sdg.getEdgeTarget(arc)).getNodeType() == NodeType.ACTUAL_OUT)
                        .map(arc -> (ActualIONode) sdg.getEdgeTarget(arc))
                        .filter(actualIONode -> actualIONode.matchesFormalIO(outFormalNode))
                        .findFirst();

                if (optionalIn.isEmpty() || optionalOut.isEmpty()) {
                    continue;
                }

                sdg.addSummaryArc(optionalIn.get(), optionalOut.get());
            }
        }
    }

    protected Set<SummaryArcPair> computeSummaryArcs(CallableDeclaration<?> declaration) {
        // Para cada formal-out, hacer slice intraprocedural. Anotar los formal-in incluidos. IMPORTANTE, SUMMARY SON INTRAPROC.
        // TODO
        return null;
    }
}
