package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.ObjectTree;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.VariableAction.DeclarationType;
import es.upv.mist.slicing.nodes.VariableAction.Definition;
import es.upv.mist.slicing.nodes.VariableAction.Movable;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.*;
import java.util.stream.Stream;

/** An interprocedural definition finder, which adds the associated actions to formal and actual nodes in the CFGs. */
public class InterproceduralDefinitionFinder extends InterproceduralActionFinder<Definition> {
    public InterproceduralDefinitionFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph, cfgMap);
    }

    @Override
    protected void handleFormalAction(CallGraph.Vertex vertex, Definition def) {
        CFG cfg = cfgMap.get(vertex.getDeclaration());
        if (!def.isParameter() || !def.isPrimitive()) {
            FormalIONode formalOut = FormalIONode.createFormalOut(vertex.getDeclaration(), def.getName());
            Movable movable = new Movable(def.toUsage(cfg.getExitNode()), formalOut);
            cfg.getExitNode().addVariableAction(movable);
        }
        FormalIONode formalIn = FormalIONode.createFormalInDecl(vertex.getDeclaration(), def.getName());
        cfg.getRootNode().addVariableAction(new Movable(def.toDeclaration(cfg.getRootNode()), formalIn));
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, Definition def) {
        List<Movable> movables = new LinkedList<>();
        GraphNode<?> graphNode = edge.getGraphNode();
        if (def.isParameter()) {
            Expression arg = extractArgument(def, edge, false);
            if (arg == null)
                return;
            ActualIONode actualOut = ActualIONode.createActualOut(edge.getCall(), def.getName(), arg);
            extractOutputVariablesAsMovables(arg, movables, graphNode, actualOut, def);
        } else if (def.isField()) {
            if (def.isStatic()) {
                // Known limitation: static fields
            } else {
                /* NEW */
                // An object creation expression doesn't alter an existing object via actual-out
                // it is returned and assigned via -output-.
                if (edge.getCall() instanceof ObjectCreationExpr)
                    return;

                ActualIONode actualOut = ActualIONode.createActualOut(edge.getCall(), def.getName(), null);
                Optional<Expression> scope = ASTUtils.getResolvableScope(edge.getCall());
                if (scope.isPresent()) {
                    extractOutputVariablesAsMovables(scope.get(), movables, graphNode, actualOut, def);
                } else {
                    var movableDef = new Definition(DeclarationType.FIELD, "this", graphNode, (ObjectTree) def.getObjectTree().clone());
                    movables.add(new Movable(movableDef, actualOut));
                }
            }
        } else {
            throw new IllegalStateException("Definition must be either from a parameter or a field!");
        }
        graphNode.addActionsForCall(movables, edge.getCall(), false);
    }

    protected void extractOutputVariablesAsMovables(Expression e, List<VariableAction.Movable> movables, GraphNode<?> graphNode, ActualIONode actualOut, VariableAction def) {
        Set<Expression> defExpressions = new HashSet<>();
        e.accept(new OutNodeVariableVisitor(), defExpressions);
        for (Expression expression : defExpressions) {
            DeclarationType type = DeclarationType.valueOf(expression);
            Definition inner = new Definition(type, expression.toString(), graphNode, (ObjectTree) def.getObjectTree().clone());
            if (defExpressions.size() > 1)
                inner.setOptional(true);
            movables.add(new Movable(inner, actualOut));
        }
    }

    @Override
    protected Stream<Definition> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg) {
        return stream.filter(VariableAction::isDefinition)
                .map(VariableAction::asDefinition)
                .filter(def -> cfg.findDeclarationFor(def).isEmpty());
    }
}
