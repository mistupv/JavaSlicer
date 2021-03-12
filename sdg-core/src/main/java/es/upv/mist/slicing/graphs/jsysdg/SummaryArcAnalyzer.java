package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.sdg.AbstractSummaryArcAnalyzer;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.oo.MemberNode;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SummaryArcAnalyzer extends AbstractSummaryArcAnalyzer<SyntheticNode<?>, SyntheticNode<?>, SyntheticNode<?>> {
    public SummaryArcAnalyzer(JSysDG sdg, CallGraph graph) {
        super(sdg, graph);
    }

    @Override
    protected Set<SyntheticNode<?>> getFormalOutNodes(CallableDeclaration<?> declaration) {
        Set<SyntheticNode<?>> set = super.getFormalOutNodes(declaration);
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

    @Override
    protected Set<SyntheticNode<?>> computeFormalIn(SyntheticNode<?> formalOut) {
        Set<SyntheticNode<?>> result = new HashSet<>();
        for (GraphNode<?> graphNode : ((JSysDG) sdg).createSlicingAlgorithm().traverseProcedure(formalOut).getGraphNodes())
            if (isFormalIn(graphNode) && graphNode instanceof SyntheticNode)
                result.add((SyntheticNode<?>) graphNode);
        return result;
    }

    @Override
    protected Optional<? extends SyntheticNode<?>> findActualIn(CallGraph.Edge<?> edge, SyntheticNode<?> formalIn) {
        return sdg.incomingEdgesOf(formalIn).stream()
                .filter(Arc::isInterproceduralInputArc)
                .map(sdg::getEdgeSource)
                .filter(actualIn -> goToParent(actualIn).getAstNode() == edge.getCall())
                .map(node -> (SyntheticNode<?>) node)
                .findAny();
    }

    @Override
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
