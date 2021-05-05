package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.ExpressionObjectTreeFinder;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.VariableAction.Movable;
import es.upv.mist.slicing.nodes.VariableAction.Usage;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** An interprocedural usage finder, which adds the associated actions to formal and actual nodes in the CFGs. */
public class InterproceduralUsageFinder extends InterproceduralActionFinder<Usage> {
    public InterproceduralUsageFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph, cfgMap);
    }

    @Override
    public void save() {
        super.save();
        markTransferenceToRoot();
    }

    /** For every variable action -scope-in- or -arg-in- in the graph,
     *  runs {@link ExpressionObjectTreeFinder#locateAndMarkTransferenceToRoot(Expression, VariableAction)}. */
    protected void markTransferenceToRoot() {
        for (CallGraph.Edge<?> edge : graph.edgeSet()) {
            for (ActualIONode actualIn : locateActualInNode(edge)) {
                for (VariableAction va : edge.getGraphNode().getVariableActions()) {
                    if (va instanceof Movable && ((Movable) va).getRealNode().equals(actualIn)) {
                        ExpressionObjectTreeFinder finder = new ExpressionObjectTreeFinder(edge.getGraphNode());
                        if (va.getName().equals("-scope-in-")) {
                            Expression scope = Objects.requireNonNullElseGet(actualIn.getArgument(), ThisExpr::new);
                            finder.locateAndMarkTransferenceToRoot(scope, va);
                        } else if (va.getName().equals("-arg-in-")) {
                            finder.locateAndMarkTransferenceToRoot(actualIn.getArgument(), va);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void handleFormalAction(CallGraph.Vertex vertex, Usage use) {
        CFG cfg = cfgMap.get(vertex.getDeclaration());
        FormalIONode formalIn = FormalIONode.createFormalIn(vertex.getDeclaration(), use.getName());
        Movable movable = new Movable(use.toDefinition(cfg.getRootNode()), formalIn);
        cfg.getRootNode().addVariableAction(movable);
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, Usage use) {
        GraphNode<?> graphNode = edge.getGraphNode();
        if (use.isParameter()) {
            if (!use.isPrimitive()) {
                assert use.hasObjectTree();
                int index = ASTUtils.getMatchingParameterIndex(graph.getEdgeTarget(edge).getDeclaration(), use.getName());
                VariableAction argIn = locateArgIn(graphNode, edge.getCall(), index);
                argIn.getObjectTree().addAll(use.getObjectTree());
            }
        } else if (use.isField()) {
            if (use.isStatic()) {
                // Known limitation: static fields
            } else {
                // An object creation expression input an existing object via actual-in because it creates it.
                assert !(edge.getCall() instanceof ObjectCreationExpr);
                VariableAction scopeIn = locateScopeIn(graphNode, edge.getCall());
                scopeIn.getObjectTree().addAll(use.getObjectTree());
            }
        } else {
            throw new IllegalStateException("Definition must be either from a parameter or a field!");
        }
    }

    /** Find all actual in nodes in the given call. */
    protected Set<ActualIONode> locateActualInNode(CallGraph.Edge<?> edge) {
        return edge.getGraphNode().getSyntheticNodesInMovables().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(ActualIONode::isInput)
                .filter(actual -> ASTUtils.equalsWithRange(actual.getAstNode(), edge.getCall()))
                .collect(Collectors.toSet());
    }

    /** Find the -arg-in- variable action that corresponds to the given node, call and index. */
    protected VariableAction locateArgIn(GraphNode<?> graphNode, Resolvable<? extends ResolvedMethodLikeDeclaration> call, int index) {
        return locateActionIn(graphNode, call, index, "-arg-in-");
    }

    /** Find the -scope-in- variable action that corresponds to the given node and call. */
    protected VariableAction locateScopeIn(GraphNode<?> graphNode, Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        return locateActionIn(graphNode, call, 0, "-scope-in-");
    }

    /** Find the nth variable action from the given node and call that matches the given name. 0 represents the first occurrence. */
    protected VariableAction locateActionIn(GraphNode<?> graphNode, Resolvable<? extends ResolvedMethodLikeDeclaration> call, int index, String actionName) {
        boolean inCall = false;
        for (VariableAction va : graphNode.getVariableActions()) {
            if (va instanceof VariableAction.CallMarker && ASTUtils.equalsWithRange(((VariableAction.CallMarker) va).getCall(), call)) {
                if (((VariableAction.CallMarker) va).isEnter())
                    inCall = true;
                else
                    break; // The call has ended, can't find the action now
            }
            if (inCall && va.isDefinition() && va.getName().equals(actionName)) {
                if (index == 0)
                    return va;
                else
                    index--;
            }
        }
        throw new IllegalStateException("Could not locate " + actionName + " for call " + call + " in node " + graphNode);
    }

    @Override
    protected Stream<Usage> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg) {
        return stream.filter(VariableAction::isUsage)
                .map(VariableAction::asUsage)
                .filter(Predicate.not(cfg::isCompletelyDefined));
    }
}
