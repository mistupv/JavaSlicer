package tfm.graphs.sdg.sumarcs;

import com.github.javaparser.ast.body.MethodDeclaration;
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
            Set<GraphNode<?>> formalOutNodes = sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                    .filter(arc -> sdg.getEdgeTarget(arc).getNodeType().is(NodeType.FORMAL_OUT))
                    .map(arc -> (GraphNode<?>) sdg.getEdgeTarget(arc))
                    .collect(Collectors.toSet());

            for (GraphNode<?> formalOutNode : formalOutNodes) {
                Set<GraphNode<?>> reachableFormalInNodes = this.findReachableFormalInNodes(formalOutNode);

                Set<GraphNode<?>> actualInNodes = reachableFormalInNodes.stream().flatMap(this::getActualInsStream).collect(Collectors.toSet());
                Set<GraphNode<?>> actualOutNodes = this.getActualOuts(formalOutNode);

                for (GraphNode<?> actualOutNode : actualOutNodes) {
                    for (GraphNode<?> actualInNode : actualInNodes) {
                        if (this.belongToSameMethodCall(actualInNode, actualOutNode)) {
                            sdg.addSummaryArc(actualInNode, actualOutNode);
                        }
                    }
                }
            }
        }
    }

    private Set<GraphNode<?>> findReachableFormalInNodes(GraphNode<?> formalOutNode) {
        return this.doFindReachableFormalInNodes(formalOutNode, Utils.emptySet());
    }

    private Set<GraphNode<?>> doFindReachableFormalInNodes(GraphNode<?> root, Set<Long> visited) {
        visited.add(root.getId());

        Set<GraphNode<?>> res = Utils.emptySet();

        if (root.getNodeType().is(NodeType.FORMAL_IN)) {
            res.add(root);
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

    private Stream<GraphNode<?>> getActualInsStream(GraphNode<?> formalIn) {
        return sdg.incomingEdgesOf(formalIn).stream()
                .filter(Arc::isParameterInOutArc)
                .filter(arc -> sdg.getEdgeSource(arc).getNodeType().is(NodeType.ACTUAL_IN))
                .map(arc -> sdg.getEdgeSource(arc));
    }

    private Set<GraphNode<?>> getActualOuts(GraphNode<?> formalOut) {
        return sdg.outgoingEdgesOf(formalOut).stream()
                .filter(Arc::isParameterInOutArc)
                .filter(arc -> sdg.getEdgeTarget(arc).getNodeType().is(NodeType.ACTUAL_OUT))
                .map(arc -> (GraphNode<?>) sdg.getEdgeTarget(arc))
                .collect(Collectors.toSet());
    }

    private boolean belongToSameMethodCall(GraphNode<?> actualIn, GraphNode<?> actualOut) {
        Optional<GraphNode<?>> optionalInCallNode = this.getCallNode(actualIn);
        Optional<GraphNode<?>> optionalOutCallNode = this.getCallNode(actualOut);

        return optionalInCallNode.isPresent() && optionalOutCallNode.isPresent()
                && optionalInCallNode.get() == optionalOutCallNode.get();
    }

    private Optional<GraphNode<?>> getCallNode(GraphNode<?> actualInOrOut) {
        return sdg.incomingEdgesOf(actualInOrOut).stream()
                .filter(arc -> sdg.getEdgeSource(arc).getNodeType().is(NodeType.METHOD_CALL))
                .map(arc -> sdg.getEdgeSource(arc))
                .findFirst()
                .map(node -> (GraphNode<?>) node);
    }
}
