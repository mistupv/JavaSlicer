package tfm.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import tfm.graphs.CallGraph;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.nodes.VariableAction;
import tfm.nodes.io.ActualIONode;
import tfm.nodes.io.FormalIONode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** An interprocedural definition finder, which adds the associated actions to formal and actual nodes in the CFGs. */
public class InterproceduralDefinitionFinder extends InterproceduralActionFinder<VariableAction.Definition> {
    public InterproceduralDefinitionFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph, cfgMap);
    }

    @Override
    protected void handleFormalAction(CallableDeclaration<?> declaration, VariableAction.Definition def) {
        CFG cfg = cfgMap.get(declaration);
        ResolvedValueDeclaration resolved = def.getNameExpr().resolve();
        if (!resolved.isParameter() || !resolved.getType().isPrimitive()) {
            FormalIONode formalOut = FormalIONode.createFormalOut(declaration, resolved);
            cfg.getExitNode().addMovableVariable(new VariableAction.Movable(def.toUsage(cfg.getExitNode()), formalOut));
        }
        FormalIONode formalIn = FormalIONode.createFormalInDecl(declaration, resolved);
        cfg.getRootNode().addMovableVariable(new VariableAction.Movable(def.toDeclaration(cfg.getRootNode()), formalIn));
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, VariableAction.Definition def) {
        Set<VariableAction.Movable> movables = new HashSet<>();
        GraphNode<?> graphNode = edge.getGraphNode();
        ResolvedValueDeclaration resolved = def.getNameExpr().resolve();
        Expression arg = extractArgument(def, edge, false);
        if (arg == null)
            return;
        ActualIONode actualOut = ActualIONode.createActualOut(edge.getCall(), resolved, arg);
        if (resolved.isParameter()) {
            Set<NameExpr> exprSet = new HashSet<>();
            arg.accept(new OutNodeVariableVisitor(), exprSet);
            for (NameExpr nameExpr : exprSet)
                movables.add(new VariableAction.Movable(new VariableAction.Definition(nameExpr, graphNode), actualOut));
        } else {
            movables.add(new VariableAction.Movable(def.toDefinition(graphNode), actualOut));
        }
        graphNode.addActionsForCall(movables, edge.getCall(), false);
    }

    @Override
    protected Set<StoredAction<VariableAction.Definition>> initialValue(CallGraph.Vertex vertex) {
        CFG cfg = cfgMap.get(vertex.getDeclaration());
        if (cfg == null)
            return Collections.emptySet();
        return cfg.vertexSet().stream()
                .filter(n -> n != cfg.getRootNode())
                .flatMap(n -> n.getVariableActions().stream())
                .filter(VariableAction::isDefinition)
                .filter(Predicate.not(VariableAction::isSynthetic))
                .map(VariableAction::asDefinition)
                .filter(def -> cfg.findDeclarationFor(def).isEmpty())
                .map(this::wrapAction)
                .collect(Collectors.toSet());
    }
}
