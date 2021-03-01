package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;

import java.util.*;
import java.util.stream.Stream;

/** An interprocedural definition finder, which adds the associated actions to formal and actual nodes in the CFGs. */
public class InterproceduralDefinitionFinder extends InterproceduralActionFinder<VariableAction.Definition> {
    public InterproceduralDefinitionFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph, cfgMap);
    }

    @Override
    protected void handleFormalAction(CallableDeclaration<?> declaration, VariableAction.Definition def) {
        CFG cfg = cfgMap.get(declaration);
        ResolvedValueDeclaration resolved = def.getResolvedValueDeclaration();
        if (!resolved.isParameter() || !resolved.getType().isPrimitive()) {
            FormalIONode formalOut = FormalIONode.createFormalOut(declaration, resolved);
            cfg.getExitNode().addMovableVariable(new VariableAction.Movable(def.toUsage(cfg.getExitNode()), formalOut));
        }
        FormalIONode formalIn = FormalIONode.createFormalInDecl(declaration, resolved);
        cfg.getRootNode().addMovableVariable(new VariableAction.Movable(def.toDeclaration(cfg.getRootNode()), formalIn));
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, VariableAction.Definition def) {
        List<VariableAction.Movable> movables = new LinkedList<>();
        GraphNode<?> graphNode = edge.getGraphNode();
        ResolvedValueDeclaration resolved = def.getResolvedValueDeclaration();
        if (resolved.isParameter()) {
            Expression arg = extractArgument(resolved.asParameter(), edge, false);
            if (arg == null)
                return;
            ActualIONode actualOut = ActualIONode.createActualOut(edge.getCall(), resolved, arg);
            if (resolved.isParameter()) {
                Set<NameExpr> exprSet = new HashSet<>();
                arg.accept(new OutNodeVariableVisitor(), exprSet);
                for (NameExpr nameExpr : exprSet)
                    movables.add(new VariableAction.Movable(new VariableAction.Definition(nameExpr, nameExpr.toString(), graphNode), actualOut));
            } else {
                movables.add(new VariableAction.Movable(def.toDefinition(graphNode), actualOut));
            }
        } else if (resolved.isField()) {
            // Known limitation: static fields
            // An object creation expression doesn't alter an existing object via actual-out
            // it is returned and assigned via -output-.
            if (edge.getCall() instanceof ObjectCreationExpr)
                return;
            String aliasedName = obtainAliasedFieldName(def, edge);
            ActualIONode actualOut = ActualIONode.createActualOut(edge.getCall(), resolved, null);
            var movableDef  = new VariableAction.Definition(obtainScope(edge.getCall()), aliasedName, graphNode, null);
            movables.add(new VariableAction.Movable(movableDef, actualOut));
        } else {
            throw new IllegalStateException("Definition must be either from a parameter or a field!");
        }
        graphNode.addActionsForCall(movables, edge.getCall(), false);
    }

    @Override
    protected Stream<VariableAction.Definition> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg) {
        return stream.filter(VariableAction::isDefinition)
                .map(VariableAction::asDefinition)
                .filter(def -> cfg.findDeclarationFor(def).isEmpty());
    }
}
