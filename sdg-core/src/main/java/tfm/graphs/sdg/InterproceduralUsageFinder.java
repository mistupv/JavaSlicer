package tfm.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import tfm.graphs.CallGraph;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.nodes.VariableAction;
import tfm.nodes.VariableVisitor;
import tfm.nodes.io.ActualIONode;
import tfm.nodes.io.FormalIONode;

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
        ResolvedValueDeclaration resolved = use.getNameExpr().resolve();
        FormalIONode formalIn = FormalIONode.createFormalIn(declaration, resolved);
        cfg.getRootNode().addMovableVariable(new VariableAction.Movable(use.toDefinition(cfg.getRootNode()), formalIn));
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, VariableAction.Usage use) {
        Set<VariableAction.Movable> movables = new HashSet<>();
        GraphNode<?> graphNode = edge.getGraphNode();
        ResolvedValueDeclaration resolved = use.getNameExpr().resolve();
        Expression argument = extractArgument(use, edge, true);
        ActualIONode actualIn = ActualIONode.createActualIn(edge.getCall(), resolved, argument);
        argument.accept(new VariableVisitor(
                (n, name) -> movables.add(new VariableAction.Movable(new VariableAction.Declaration(name, graphNode), actualIn)),
                (n, name) -> movables.add(new VariableAction.Movable(new VariableAction.Definition(name, graphNode), actualIn)),
                (n, name) -> movables.add(new VariableAction.Movable(new VariableAction.Usage(name, graphNode), actualIn))
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
