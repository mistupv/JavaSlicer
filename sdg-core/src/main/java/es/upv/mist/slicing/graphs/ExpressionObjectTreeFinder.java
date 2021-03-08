package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.utils.Pair;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.VariableAction.ObjectTree;
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
        ResolvedReferenceType type = variableDeclarator.getType().resolve().asReferenceType();
        var fields = ClassGraph.getInstance().generateObjectTreeFor(type);
        targetAction.getObjectTree().addAll(fields);
        locateExpressionResultTrees(variableDeclarator.getInitializer().get())
                .forEach(pair -> markTransference(pair, targetAction, ""));
    }

    protected VariableAction locateVAVariableDeclarator(String varName) {
        boolean foundDecl = false;
        VariableAction lastDef = null;
        for (VariableAction a : graphNode.getVariableActions()) {
            if (a.isDeclaration()) {
                if (a.getVariable().equals(varName))
                    foundDecl = true;
                else if (foundDecl)
                    return lastDef;
            } else if (a.isDefinition() && a.getVariable().equals(varName)) {
                lastDef = a;
            }
        }
        assert lastDef != null: "Could not find DEF for variable declaration of " + varName;
        return lastDef;
    }

    public void handleAssignExpr(AssignExpr assignExpr, VariableAction assignTarget, String targetMember) {
        ResolvedReferenceType type = assignExpr.getTarget().calculateResolvedType().asReferenceType();
        var fields = ClassGraph.getInstance().generateObjectTreeFor(type);
        assignTarget.getObjectTree().addAll(fields);
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
                if (n.resolve().isType())
                    return;
                for (VariableAction action : graphNode.getVariableActions()) {
                    if (action.isUsage() &&
                            action.getVariable().equals(n.getNameAsString()) &&
                            n.equals(action.getVariableExpression())) {
                        list.add(new Pair<>(action, arg));
                        return;
                    }
                }
                throw new IllegalStateException("Cannot find USE action for var " + n);
            }

            @Override
            public void visit(ThisExpr n, String arg) {
                for (VariableAction action : graphNode.getVariableActions()) {
                    if (action.isUsage()) {
                        if (action.getVariableExpression() == null) {
                            if (action.getVariable().matches("^.*this$")) {
                                list.add(new Pair<>(action, arg));
                                return;
                            }
                        } else {
                            if (n.equals(action.getVariableExpression())) {
                                list.add(new Pair<>(action, arg));
                                return;
                            }
                        }
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
                for (VariableAction variableAction : graphNode.getVariableActions()) {
                    if (variableAction.isUsage() &&
                            variableAction.getVariable().equals(VARIABLE_NAME_OUTPUT) &&
                            call.equals(variableAction.getVariableExpression())) {
                        list.add(new Pair<>(variableAction, arg));
                        return;
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
        ObjectTree.copyTree(sourceAction.getObjectTree(), targetAction.getObjectTree(), sourceMember, targetMember);
        sourceAction.setPDGTreeConnectionTo(targetAction, sourceMember, targetMember);
    }
}
