package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.VariableVisitor;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
        List<VariableAction.Movable> movables = new LinkedList<>();
        GraphNode<?> graphNode = edge.getGraphNode();
        ResolvedValueDeclaration resolved = use.getResolvedValueDeclaration();
        if (resolved.isParameter()) {
            Expression argument = extractArgument(resolved.asParameter(), edge, true);
            ActualIONode actualIn = ActualIONode.createActualIn(edge.getCall(), resolved, argument);
            argument.accept(new VariableVisitor(
                    (n, exp, name) -> movables.add(new VariableAction.Movable(new VariableAction.Declaration(exp, name, graphNode), actualIn)),
                    (n, exp, name, expression) -> movables.add(new VariableAction.Movable(new VariableAction.Definition(exp, name, graphNode, expression), actualIn)),
                    (n, exp, name) -> movables.add(new VariableAction.Movable(new VariableAction.Usage(exp, name, graphNode), actualIn))
            ), VariableVisitor.Action.USE);
        } else if (resolved.isField()) {
            // Known limitation: static fields
            // An object creation expression input an existing object via actual-in because it creates it.
            if (edge.getCall() instanceof ObjectCreationExpr)
                return;
            String aliasedName = obtainAliasedFieldName(use, edge);
            ActualIONode actualIn = ActualIONode.createActualIn(edge.getCall(), resolved, null);
            var movableUse = new VariableAction.Usage(obtainScope(edge.getCall()), aliasedName, graphNode);
            movables.add(new VariableAction.Movable(movableUse, actualIn));
        } else {
            throw new IllegalStateException("Definition must be either from a parameter or a field!");
        }
        graphNode.addActionsForCall(movables, edge.getCall(), true);
    }

    @Override
    protected Stream<VariableAction.Usage> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg) {
        return stream.filter(VariableAction::isUsage)
                .map(VariableAction::asUsage)
                .filter(Predicate.not(cfg::isCompletelyDefined));
    }
}
