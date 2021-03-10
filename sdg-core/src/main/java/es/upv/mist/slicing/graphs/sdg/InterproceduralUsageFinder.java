package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.ExpressionObjectTreeFinder;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.ObjectTree;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.VariableAction.Definition;
import es.upv.mist.slicing.nodes.VariableAction.Movable;
import es.upv.mist.slicing.nodes.VariableAction.Usage;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.Map;
import java.util.Objects;
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
        FormalIONode formalIn = FormalIONode.createFormalIn(vertex.getDeclaration(), use.getName());
        Movable movable = new Movable(use.toDefinition(cfg.getRootNode()), formalIn);
        cfg.getRootNode().addVariableAction(movable);
    }

    @Override
    protected void handleActualAction(CallGraph.Edge<?> edge, Usage use) {
        GraphNode<?> graphNode = edge.getGraphNode();
        if (use.isParameter()) {
            ActualIONode actualIn = locateActualInNode(edge, use.getName());
            Definition def = new Definition(VariableAction.DeclarationType.SYNTHETIC, "-arg-in-", graphNode, (ObjectTree) use.getObjectTree().clone());
            Movable movDef = new Movable(def, actualIn);
            graphNode.addVariableActionAfterLastMatchingRealNode(movDef, actualIn);
            ExpressionObjectTreeFinder finder = new ExpressionObjectTreeFinder(graphNode);
            finder.locateAndMarkTransferenceToRoot(actualIn.getArgument(), def);
        } else if (use.isField()) {
            if (use.isStatic()) {
                // Known limitation: static fields
            } else {
                // An object creation expression input an existing object via actual-in because it creates it.
                if (edge.getCall() instanceof ObjectCreationExpr)
                    return;
                ActualIONode actualIn = locateActualInNode(edge, use.getName());
                Definition def = new Definition(VariableAction.DeclarationType.SYNTHETIC, "-scope-in-", graphNode, (ObjectTree) use.getObjectTree().clone());
                Movable movDef = new Movable(def, actualIn);
                Expression scope = Objects.requireNonNullElseGet(actualIn.getArgument(), ThisExpr::new);
                graphNode.addVariableActionAfterLastMatchingRealNode(movDef, actualIn);
                ExpressionObjectTreeFinder finder = new ExpressionObjectTreeFinder(graphNode);
                finder.locateAndMarkTransferenceToRoot(scope, def);
            }
        } else {
            throw new IllegalStateException("Definition must be either from a parameter or a field!");
        }
    }

    protected ActualIONode locateActualInNode(CallGraph.Edge<?> edge, String name) {
        return edge.getGraphNode().getSyntheticNodesInMovables().stream()
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .filter(ActualIONode::isInput)
                .filter(actual -> actual.getVariableName().equals(name))
                .filter(actual -> ASTUtils.equalsWithRange(actual.getAstNode(), (Node) edge.getCall()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("can't locate actual-in node"));
    }

    @Override
    protected Stream<Usage> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg) {
        return stream.filter(VariableAction::isUsage)
                .map(VariableAction::asUsage)
                .filter(Predicate.not(cfg::isCompletelyDefined));
    }
}
