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

    public void handleVariableDeclarator(VariableDeclarator variableDeclarator, String realName) {
        assert variableDeclarator.getInitializer().isPresent();
        VariableAction targetAction = locateVAVariableDeclarator(realName);
        ClassGraph.getInstance().generateObjectTreeForType(variableDeclarator.getType().resolve())
                .ifPresent(objectTree -> targetAction.getObjectTree().addAll(objectTree));
        locateExpressionResultTrees(variableDeclarator.getInitializer().get())
                .forEach(pair -> markTransference(pair, targetAction, ""));
    }

    protected VariableAction locateVAVariableDeclarator(String realName) {
        String root = realName.contains(".") ? ObjectTree.removeFields(realName) : realName;
        boolean foundDecl = false;
        VariableAction lastDef = null;
        for (VariableAction a : graphNode.getVariableActions()) {
            if (a.isDeclaration()) {
                if (a.getName().equals(realName))
                    foundDecl = true;
                else if (foundDecl)
                    return lastDef;
            } else if (a.isDefinition() && a.getName().equals(root)) {
                if (root.equals(realName) || a.hasTreeMember(realName))
                    lastDef = a;
            }
        }
        assert lastDef != null: "Could not find DEF for variable declaration of " + realName;
        return lastDef;
    }

    public void handleAssignExpr(AssignExpr assignExpr, VariableAction assignTarget, String targetMember) {
        ClassGraph.getInstance().generateObjectTreeForType(assignExpr.getTarget().calculateResolvedType())
                .ifPresent(fields -> assignTarget.getObjectTree().addAll(fields));
        locateExpressionResultTrees(assignExpr.getValue())
                .forEach(pair -> markTransference(pair, assignTarget, targetMember));
    }

    public void handleArrayAssignExpr(AssignExpr assignExpr) {
        locateExpressionResultTrees(assignExpr.getValue())
                .forEach(pair -> pair.a.setPDGValueConnection(pair.b));
    }

    public void locateAndMarkTransferenceToRoot(Expression expr, int index) {
        List<VariableAction> list = graphNode.getVariableActions();
        locateAndMarkTransferenceToRoot(expr, list.get(index < 0 ? list.size() + index : index));
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
                n.getName().accept(this, arg);
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

            @Override
            public void visit(ArrayCreationExpr n, String arg) {}

            @Override
            public void visit(ArrayInitializerExpr n, String arg) {}

            @Override
            public void visit(BinaryExpr n, String arg) {}

            @Override
            public void visit(ClassExpr n, String arg) {}

            @Override
            public void visit(InstanceOfExpr n, String arg) {}

            @Override
            public void visit(UnaryExpr n, String arg) {}

            @Override
            public void visit(LambdaExpr n, String arg) {}

            @Override
            public void visit(MethodReferenceExpr n, String arg) {}

            @Override
            public void visit(PatternExpr n, String arg) {}
        }, "");
        return list;
    }

    protected void markTransference(Pair<VariableAction, String> sourcePair, VariableAction targetAction, String targetMember) {
        VariableAction sourceAction = sourcePair.a;
        String sourceMember = sourcePair.b;
        if (targetAction.hasObjectTree()) {
            ObjectTree.copyTargetTreeToSource(sourceAction.getObjectTree(), targetAction.getObjectTree(), sourceMember, targetMember);
            sourceAction.setPDGTreeConnectionTo(targetAction, sourceMember, targetMember);
        } else {
            sourceAction.setPDGValueConnection(sourceMember);
        }
    }
}
