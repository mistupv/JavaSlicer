package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
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
            Optional<Expression> arg = extractArgument(def, edge, false);
            if (arg.isEmpty())
                return;
            ActualIONode actualOut = locateActualOutNode(edge, def.getName())
                    .orElseGet(() -> ActualIONode.createActualOut(edge.getCall(), def.getName(), arg.get()));
            extractOutputVariablesAsMovables(arg.get(), movables, graphNode, actualOut, def);
        } else if (def.isField()) {
            if (def.isStatic()) {
                // Known limitation: static fields
            } else {
                assert !(edge.getCall() instanceof ObjectCreationExpr);
                ActualIONode actualOut = locateActualOutNode(edge, def.getName())
                        .orElseGet(() -> ActualIONode.createActualOut(edge.getCall(), def.getName(), null));
                Optional<Expression> scope = ASTUtils.getResolvableScope(edge.getCall());
                if (scope.isPresent() && !(scope.get() instanceof SuperExpr) && !(scope.get() instanceof ThisExpr)) {
                    extractOutputVariablesAsMovables(scope.get(), movables, graphNode, actualOut, def);
                } else {
                    assert def.hasObjectTree();
                    Optional<VariableAction> optVA = locateDefinition(graphNode, "this");
                    if (optVA.isPresent())
                        optVA.get().getObjectTree().addAll(def.getObjectTree());
                    else {
                        var movableDef = new Definition(DeclarationType.FIELD, "this", graphNode, (ObjectTree) def.getObjectTree().clone());
                        movables.add(new Movable(movableDef, actualOut));
                    }
                }
            }
        } else {
            throw new IllegalStateException("Definition must be either from a parameter or a field!");
        }
        graphNode.addActionsForCall(movables, edge.getCall(), false);
    }

    /** For each variable of an expression that may be passed through it (i.e., that if passed as argument of a function, could
     *  a modification to that reference affect any part of the variables passed as input?). It then generates the necessary
     *  definitions and links between trees in order to define each of them as a function of the given actual out. */
    protected void extractOutputVariablesAsMovables(Expression e, List<VariableAction.Movable> movables, GraphNode<?> graphNode, ActualIONode actualOut, VariableAction def) {
        Set<Expression> defExpressions = new HashSet<>();
        e.accept(new OutNodeVariableVisitor(), defExpressions);
        for (Expression expression : defExpressions) {
            assert def.hasObjectTree();
            Optional<VariableAction> optVa = locateDefinition(graphNode, expression.toString());
            if (optVa.isPresent()) {
                optVa.get().getObjectTree().addAll(def.getObjectTree());
            } else {
                DeclarationType type = DeclarationType.valueOf(expression);
                Definition inner = new Definition(type, expression.toString(), graphNode, (ObjectTree) def.getObjectTree().clone());
                if (defExpressions.size() > 1)
                    inner.setOptional(true);
                movables.add(new Movable(inner, actualOut));
            }
        }
    }

    /** Find the actual out node in the given edge call that corresponds to the given variable name. */
    protected Optional<ActualIONode> locateActualOutNode(CallGraph.Edge<?> edge, String name) {
        return edge.getGraphNode().getSyntheticNodesInMovables().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(ActualIONode::isOutput)
                .filter(actual -> actual.getVariableName().equals(name))
                .filter(actual -> ASTUtils.equalsWithRange(actual.getAstNode(), edge.getCall()))
                .findFirst();
    }

    /** Try to locate the definition for the given variable name in the given node. */
    protected Optional<VariableAction> locateDefinition(GraphNode<?> graphNode, String name) {
        return graphNode.getVariableActions().stream()
                .filter(va -> va.getName().equals(name))
                .filter(VariableAction::isDefinition)
                .findAny();
    }

    @Override
    protected Stream<Definition> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg) {
        return stream.filter(VariableAction::isDefinition)
                .map(VariableAction::asDefinition)
                .filter(def -> cfg.findDeclarationFor(def).isEmpty());
    }
}
