package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.VariableAction.*;
import es.upv.mist.slicing.nodes.VariableVisitor;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** An interprocedural usage finder, which adds the associated actions to formal and actual nodes in the CFGs. */
public class InterproceduralUsageFinder extends InterproceduralActionFinder<Usage> {
    public InterproceduralUsageFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph, cfgMap);
    }

    @Override
    protected void handleFormalAction(CallGraph.Vertex vertex, Usage use) {
        CFG cfg = cfgMap.get(vertex.getDeclaration());
        ResolvedValueDeclaration resolved = use.getResolvedValueDeclaration();
        FormalIONode formalIn = FormalIONode.createFormalIn(vertex.getDeclaration(), resolved);
        ObjectTree objTree = vertexDataMap.get(vertex).get(use);
        Movable movable = new Movable(use.toDefinition(cfg.getRootNode(), objTree), formalIn);
        cfg.getRootNode().addMovableVariable(movable);
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, Usage use) {
        List<Movable> movables = new LinkedList<>();
        GraphNode<?> graphNode = edge.getGraphNode();
        ResolvedValueDeclaration resolved = use.getResolvedValueDeclaration();
        ObjectTree objTree = vertexDataMap.get(graph.getEdgeTarget(edge)).get(use);
        if (resolved.isParameter()) {
            Expression argument = extractArgument(resolved.asParameter(), edge, true);
            ActualIONode actualIn = ActualIONode.createActualIn(edge.getCall(), resolved, argument);
            argument.accept(new VariableVisitor(
                    (n, exp, name) -> movables.add(new Movable(new Declaration(exp, name, graphNode), actualIn)),
                    (n, exp, name, expression) -> movables.add(new Movable(new Definition(exp, name, graphNode, expression), actualIn)),
                    (n, exp, name) -> movables.add(new Movable(new Usage(exp, name, graphNode), actualIn))
            ), VariableVisitor.Action.USE);
            // a) es un objeto: movables==1 ('a', 'this')
            // b) es una combinacion otras cosas ('a[1]', 'a.call()', construccion de string)
            // void f(A a) { log(a.x); } <-- f(theA); // se copia el arbol de log(a) a f(theA) :D
            // void f(A a) { log(a.x); } <-- f(as[1]); // no se debe copiar el arbol a as
            // void f(A a) { log(a.x); } <-- f(call()); // el arbol va de log a call (por su retorno)
            // TODO: this check is not specific enough
            // Only copy the tree to the movables if there is only 1 movable: it is an object.
            if (movables.size() == 1)
                movables.get(0).getObjectTree().addAll(objTree);
        } else if (resolved.isField()) {
            // Known limitation: static fields
            // An object creation expression input an existing object via actual-in because it creates it.
            if (edge.getCall() instanceof ObjectCreationExpr)
                return;
            String aliasedName = obtainAliasedFieldName(use, edge);
            ActualIONode actualIn = ActualIONode.createActualIn(edge.getCall(), resolved, null);
            var movableUse = new Usage(obtainScope(edge.getCall()), aliasedName, graphNode, objTree);
            movables.add(new Movable(movableUse, actualIn));
        } else {
            throw new IllegalStateException("Definition must be either from a parameter or a field!");
        }
        graphNode.addActionsForCall(movables, edge.getCall(), true);
    }

    @Override
    protected Stream<Usage> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg) {
        return stream.filter(VariableAction::isUsage)
                .map(VariableAction::asUsage)
                .filter(Predicate.not(cfg::isCompletelyDefined));
    }
}
