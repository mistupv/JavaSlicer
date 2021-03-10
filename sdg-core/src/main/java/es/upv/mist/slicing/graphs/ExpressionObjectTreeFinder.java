package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.utils.Pair;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.ObjectTree;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.LinkedList;
import java.util.List;

import static es.upv.mist.slicing.graphs.cfg.CFGBuilder.VARIABLE_NAME_OUTPUT;

public class ExpressionObjectTreeFinder {

    protected final GraphNode<?> graphNode;

    public ExpressionObjectTreeFinder(GraphNode<?> graphNode) {
        this.graphNode = graphNode;
    }

    public void handleVariableDeclarator(VariableDeclarator variableDeclarator) {
        assert variableDeclarator.getInitializer().isPresent();
        VariableAction targetAction = locateVAVariableDeclarator(variableDeclarator.getNameAsString());
        ClassGraph.getInstance().generateObjectTreeForType(variableDeclarator.getType().resolve())
                .ifPresent(objectTree -> targetAction.getObjectTree().addAll(objectTree));
        locateExpressionResultTrees(variableDeclarator.getInitializer().get())
                .forEach(pair -> markTransference(pair, targetAction, ""));
    }

    protected VariableAction locateVAVariableDeclarator(String varName) {
        boolean foundDecl = false;
        VariableAction lastDef = null;
        for (VariableAction a : graphNode.getVariableActions()) {
            if (a.isDeclaration()) {
                if (a.getName().equals(varName))
                    foundDecl = true;
                else if (foundDecl)
                    return lastDef;
            } else if (a.isDefinition() && a.getName().equals(varName)) {
                lastDef = a;
            }
        }
        assert lastDef != null: "Could not find DEF for variable declaration of " + varName;
        return lastDef;
    }

    public void handleAssignExpr(AssignExpr assignExpr, VariableAction assignTarget, String targetMember) {
        ClassGraph.getInstance().generateObjectTreeForType(assignExpr.getTarget().calculateResolvedType())
                .ifPresent(fields -> assignTarget.getObjectTree().addAll(fields));
        locateExpressionResultTrees(assignExpr.getValue())
                .forEach(pair -> markTransference(pair, assignTarget, targetMember));
    }

    public void locateAndMarkTransferenceToRoot(Expression expr, int index) {
        List<VariableAction> list = graphNode.getVariableActions();
        if (index < 0)
            locateAndMarkTransferenceToRoot(expr, list.get(list.size() + index));
        else
            locateAndMarkTransferenceToRoot(expr, list.get(index));
    }

    public void locateAndMarkTransferenceToRoot(Expression expression, VariableAction targetAction) {
        locateExpressionResultTrees(expression)
                .forEach(pair -> markTransference(pair, targetAction, ""));
    }

    protected List<Pair<VariableAction, String>> locateExpressionResultTrees(Expression expression) {
        List<Pair<VariableAction, String>> list = new LinkedList<>();
        expression.accept(new VoidVisitorAdapter<String>() {
            @Override
            public void visit(ArrayAccessExpr n, String arg) {
                throw new UnsupportedOperationException("Array accesses are not supported as argument for return, call scope or argument. Please, pre-process your graph or use the EDG.");
            }

            @Override
            public void visit(AssignExpr n, String arg) {
                n.getValue().accept(this, arg);
            }

            @Override
            public void visit(ConditionalExpr n, String arg) {
                n.getThenExpr().accept(this, arg);
                n.getElseExpr().accept(this, arg);
            }

            @Override
            public void visit(NameExpr n, String arg) {
                ResolvedValueDeclaration resolved = n.resolve();
                if (resolved.isType())
                    return;
                if (resolved.isField() && !resolved.asField().isStatic()) {
                    new FieldAccessExpr(new ThisExpr(), n.getNameAsString()).accept(this, arg);
                    return;
                }
                for (VariableAction action : graphNode.getVariableActions()) {
                    if (action.isUsage() && action.getName().equals(n.getNameAsString())) {
                        list.add(new Pair<>(action, arg));
                        return;
                    }
                }
                throw new IllegalStateException("Cannot find USE action for var " + n);
            }

            @Override
            public void visit(ThisExpr n, String arg) {
                for (VariableAction action : graphNode.getVariableActions()) {
                    if (action.isUsage() && action.getName().matches("^.*this$")) {
                        list.add(new Pair<>(action, arg));
                        return;
                    }
                }
                throw new IllegalStateException("Could not find USE(this)");
            }

            @Override
            public void visit(FieldAccessExpr n, String arg) {
                if (!arg.isEmpty())
                    arg = "." + arg;
                n.getScope().accept(this, n.getNameAsString() + arg);
            }

            @Override
            public void visit(ObjectCreationExpr n, String arg) {
                visitCall(n, arg);
            }

            @Override
            public void visit(MethodCallExpr n, String arg) {
                visitCall(n, arg);
            }

            protected void visitCall(Expression call, String arg) {
                if (ASTUtils.shouldVisitArgumentsForMethodCalls((Resolvable<? extends ResolvedMethodLikeDeclaration>) call))
                    return;
                VariableAction lastUseOut = null;
                for (VariableAction variableAction : graphNode.getVariableActions()) {
                    if (variableAction instanceof VariableAction.CallMarker) {
                        VariableAction.CallMarker marker = (VariableAction.CallMarker) variableAction;
                        if (ASTUtils.equalsWithRange((Node) marker.getCall(), call) && !marker.isEnter()) {
                            assert lastUseOut != null;
                            list.add(new Pair<>(lastUseOut, arg));
                            return;
                        }
                    }
                    if (variableAction.isUsage() && variableAction.getName().equals(VARIABLE_NAME_OUTPUT)) {
                        lastUseOut = variableAction;
                    }
                }
                throw new IllegalStateException("Could not find USE(-output-) corresponding to call " + call);
            }
        }, "");
        return list;
    }

    protected void markTransference(Pair<VariableAction, String> sourcePair, VariableAction targetAction, String targetMember) {
        VariableAction sourceAction = sourcePair.a;
        String sourceMember = sourcePair.b;
        if (targetAction.hasObjectTree() &&
                (!sourceAction.hasObjectTree() || !sourceAction.getObjectTree().isLeaf(sourceMember)))
            ObjectTree.copyTargetTreeToSource(sourceAction.getObjectTree(), targetAction.getObjectTree(), sourceMember, targetMember);
        sourceAction.setPDGTreeConnectionTo(targetAction, sourceMember, targetMember);
    }
}
