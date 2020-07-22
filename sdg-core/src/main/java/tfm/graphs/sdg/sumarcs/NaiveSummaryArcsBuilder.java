package tfm.graphs.sdg.sumarcs;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import tfm.arcs.Arc;
import tfm.graphs.sdg.SDG;
import tfm.nodes.GraphNode;
import tfm.nodes.SyntheticNode;
import tfm.nodes.type.NodeType;
import tfm.utils.Utils;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NaiveSummaryArcsBuilder extends SummaryArcsBuilder {

    public NaiveSummaryArcsBuilder(SDG sdg) {
        super(sdg);
    }

    @SuppressWarnings("unchecked")
    protected Collection<GraphNode<MethodDeclaration>> findAllMethodDeclarations() {
        return sdg.vertexSet().stream()
                .filter(Predicate.not((SyntheticNode.class::isInstance)))
                .filter(n -> n.getAstNode() instanceof MethodDeclaration)
                .map(n -> (GraphNode<MethodDeclaration>) n)
                .collect(Collectors.toSet());
    }

    @Override
    public void visit() {
        for (GraphNode<MethodDeclaration> methodDeclarationNode : findAllMethodDeclarations()) {
            Set<GraphNode<ExpressionStmt>> formalOutNodes = sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                    .filter(arc -> sdg.getEdgeTarget(arc).getNodeType() == NodeType.FORMAL_OUT)
                    .map(arc -> (GraphNode<ExpressionStmt>) sdg.getEdgeTarget(arc))
                    .collect(Collectors.toSet());

            for (GraphNode<ExpressionStmt> formalOutNode : formalOutNodes) {
                Set<GraphNode<ExpressionStmt>> reachableFormalInNodes = this.findReachableFormalInNodes(formalOutNode);

                Set<GraphNode<ExpressionStmt>> actualInNodes = reachableFormalInNodes.stream().flatMap(this::getActualInsStream).collect(Collectors.toSet());
                Set<GraphNode<ExpressionStmt>> actualOutNodes = this.getActualOuts(formalOutNode);

                for (GraphNode<ExpressionStmt> actualOutNode : actualOutNodes) {
                    for (GraphNode<ExpressionStmt> actualInNode : actualInNodes) {
                        if (this.belongToSameMethodCall(actualInNode, actualOutNode)) {
                            sdg.addSummaryArc(actualInNode, actualOutNode);
                        }
                    }
                }
            }
        }
    }

    private Set<GraphNode<ExpressionStmt>> findReachableFormalInNodes(GraphNode<ExpressionStmt> formalOutNode) {
        return this.doFindReachableFormalInNodes(formalOutNode, Utils.emptySet());
    }

    private Set<GraphNode<ExpressionStmt>> doFindReachableFormalInNodes(GraphNode<?> root, Set<Long> visited) {
        visited.add(root.getId());

        Set<GraphNode<ExpressionStmt>> res = Utils.emptySet();

        if (root.getNodeType() == NodeType.FORMAL_IN) {
            res.add((GraphNode<ExpressionStmt>) root);
        } else {
            for (Arc arc : sdg.incomingEdgesOf(root)) {
                GraphNode<?> nextNode = sdg.getEdgeSource(arc);

                if (visited.contains(nextNode.getId())) {
                    continue;
                }

                if (arc.isControlDependencyArc() || arc.isDataDependencyArc()) {
                    res.addAll(this.doFindReachableFormalInNodes(nextNode, visited));
                }
            }
        }

        return res;
    }

    private Stream<GraphNode<ExpressionStmt>> getActualInsStream(GraphNode<ExpressionStmt> formalIn) {
        return sdg.incomingEdgesOf(formalIn).stream()
                .filter(Arc::isParameterInOutArc)
                .filter(arc -> sdg.getEdgeSource(arc).getNodeType() == NodeType.ACTUAL_IN)
                .map(arc -> (GraphNode<ExpressionStmt>) sdg.getEdgeSource(arc));
    }

    private Set<GraphNode<ExpressionStmt>> getActualOuts(GraphNode<ExpressionStmt> formalOut) {
        return sdg.outgoingEdgesOf(formalOut).stream()
                .filter(Arc::isParameterInOutArc)
                .filter(arc -> sdg.getEdgeTarget(arc).getNodeType() == NodeType.ACTUAL_OUT)
                .map(arc -> (GraphNode<ExpressionStmt>) sdg.getEdgeTarget(arc))
                .collect(Collectors.toSet());
    }

    private boolean belongToSameMethodCall(GraphNode<ExpressionStmt> actualIn, GraphNode<ExpressionStmt> actualOut) {
        Optional<GraphNode<ExpressionStmt>> optionalInCallNode = this.getCallNode(actualIn);
        Optional<GraphNode<ExpressionStmt>> optionalOutCallNode = this.getCallNode(actualOut);

        return optionalInCallNode.isPresent() && optionalOutCallNode.isPresent()
                && optionalInCallNode.get() == optionalOutCallNode.get();
    }

    private Optional<GraphNode<ExpressionStmt>> getCallNode(GraphNode<ExpressionStmt> actualInOrOut) {
        return sdg.incomingEdgesOf(actualInOrOut).stream()
                .filter(arc -> sdg.getEdgeSource(arc).getNodeType() == NodeType.METHOD_CALL)
                .map(arc -> (GraphNode<ExpressionStmt>) sdg.getEdgeSource(arc))
                .findFirst();
    }
}
