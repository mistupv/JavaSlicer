package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.VariableVisitor;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** An interprocedural usage finder, which adds the associated actions to formal and actual nodes in the CFGs. */
public class InterproceduralUsageFinder extends InterproceduralActionFinder<VariableAction.Usage> {
    public InterproceduralUsageFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph, cfgMap);
    }

    @Override
    protected void handleFormalAction(CallableDeclaration<?> declaration, VariableAction.Usage use) {
        CFG cfg = cfgMap.get(declaration);
        ResolvedValueDeclaration resolved = use.getResolvedValueDeclaration();
        FormalIONode formalIn = FormalIONode.createFormalIn(declaration, resolved);
        cfg.getRootNode().addMovableVariable(new VariableAction.Movable(use.toDefinition(cfg.getRootNode()), formalIn));
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, VariableAction.Usage use) {
        Set<VariableAction.Movable> movables = new HashSet<>();
        GraphNode<?> graphNode = edge.getGraphNode();
        ResolvedValueDeclaration resolved = use.getResolvedValueDeclaration();
        Expression argument = extractArgument(use, edge, true);
        ActualIONode actualIn = ActualIONode.createActualIn(edge.getCall(), resolved, argument);
        argument.accept(new VariableVisitor(
                (n, exp, name) -> movables.add(new VariableAction.Movable(new VariableAction.Declaration(exp, name, graphNode), actualIn)),
                (n, exp, name, expression) -> movables.add(new VariableAction.Movable(new VariableAction.Definition(exp, name, graphNode, expression), actualIn)),
                (n, exp, name) -> movables.add(new VariableAction.Movable(new VariableAction.Usage(exp, name, graphNode), actualIn))
        ), VariableVisitor.Action.USE);
        graphNode.addActionsForCall(movables, edge.getCall(), true);
    }

    @Override
    protected Set<StoredAction<VariableAction.Usage>> initialValue(CallGraph.Vertex vertex) {
        CFG cfg = cfgMap.get(vertex.getDeclaration());
        if (cfg == null)
            return Collections.emptySet();
        return cfg.vertexSet().stream()
                .filter(n -> n != cfg.getRootNode())
                .flatMap(n -> n.getVariableActions().stream())
                .filter(VariableAction::isUsage)
                .filter(Predicate.not(VariableAction::isSynthetic))
                .map(VariableAction::asUsage)
                .filter(Predicate.not(cfg::isCompletelyDefined))
                .map(this::wrapAction)
                .collect(Collectors.toSet());
    }
}
