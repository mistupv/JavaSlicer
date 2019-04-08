package tfm.utils;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGNode;
import tfm.variables.actions.VariableAction;

import java.util.ArrayList;
import java.util.List;


public class DataDependencyVisitor {

    public static void visit(Expression expression, PDGGraph graph, PDGNode currentNode) {
        VariableVisitor variableVisitor = new VariableVisitor(graph, currentNode);
        expression.accept(variableVisitor, VariableAction.Actions.USE);
    }

    private static class VariableVisitor extends VoidVisitorAdapter<VariableAction.Actions> {

        private PDGGraph pdgGraph;
        private PDGNode currentNode;

        private List<String> definedVariables;

        private VariableVisitor(PDGGraph graph, PDGNode currentNode) {
            this.pdgGraph = graph;
            this.currentNode = currentNode;
            this.definedVariables = new ArrayList<>();
        }

        @Override
        public void visit(ArrayAccessExpr n, VariableAction.Actions action) {
            Logger.log("On ArrayAccessExpr: [" + n + "]");
            n.getName().accept(this, action.or(VariableAction.Actions.USE));
            n.getIndex().accept(this, action.or(VariableAction.Actions.USE));
        }

        @Override
        public void visit(AssignExpr n, VariableAction.Actions action) {
            Logger.log("On AssignExpr: [" + n + "]");
            n.getTarget().accept(this, action.or(VariableAction.Actions.DEFINITION));
            n.getValue().accept(this, action.or(VariableAction.Actions.USE));
        }

        @Override
        public void visit(BinaryExpr n, VariableAction.Actions action) {
            Logger.log("On BinaryExpr: [" + n + "]");
            n.getLeft().accept(this, action.or(VariableAction.Actions.USE));
            n.getRight().accept(this, action.or(VariableAction.Actions.USE));
        }

        @Override
        public void visit(CastExpr n, VariableAction.Actions action) {
            Logger.log("On CastExpr: [" + n + "]");
            n.getExpression().accept(this, action.or(VariableAction.Actions.USE));
        }

        @Override
        public void visit(ConditionalExpr n, VariableAction.Actions action) {
            Logger.log("On ConditionalExpr: [" + n + "]");
            n.getCondition().accept(this, action.or(VariableAction.Actions.USE));
            n.getThenExpr().accept(this, action.or(VariableAction.Actions.USE));
            n.getElseExpr().accept(this, action.or(VariableAction.Actions.USE));
        }

        @Override
        public void visit(EnclosedExpr n, VariableAction.Actions action) {
            Logger.log("On EnclosedExpr: [" + n + "]");
            n.getInner().accept(this, action.or(VariableAction.Actions.USE));
        }

        @Override
        public void visit(FieldAccessExpr n, VariableAction.Actions action) {
            Logger.log("On FieldAccessExpr: [" + n + "]");
            n.getScope().accept(this, action.or(VariableAction.Actions.USE));
        }

//        @Override
//        public void visit(InstanceOfExpr n, Actions action) {
//            ??
//        }

        // ???
        @Override
        public void visit(MethodCallExpr n, VariableAction.Actions action) {
            Logger.log("On MethodCallExpr: [" + n + "]");
            n.getScope().ifPresent(expression -> expression.accept(this, action.or(VariableAction.Actions.USE)));
            n.getArguments().forEach(expression -> expression.accept(this, action.or(VariableAction.Actions.USE)));
        }

        @Override
        public void visit(NameExpr n, VariableAction.Actions action) {
            Logger.log("On NameExpr. Found variable " + n.getNameAsString() + " and action " + action);

            String variableName = n.getNameAsString();

            if (action == VariableAction.Actions.DEFINITION) {
                definedVariables.add(variableName);
            } else if (action == VariableAction.Actions.USE) {
                // todo: create dependency from the defined variables to the variable in use
                // inside pdg graph class: find last node where the variable in use is defined
            }
        }

//        @Override
//        public void visit(NormalAnnotationExpr n, Actions action) {
//            ??
//        }

//        @Override
//        public void visit(SingleMemberAnnotationExpr n, Actions action) {
//            ??
//        }

        @Override
        public void visit(UnaryExpr n, VariableAction.Actions action) {
            Logger.log("On UnaryExpr: [" + n + "]");
            n.getExpression().accept(this, action.or(VariableAction.Actions.USE));
            n.getExpression().accept(this, action.or(VariableAction.Actions.DEFINITION));
        }

        @Override
        public void visit(VariableDeclarationExpr n, VariableAction.Actions action) {
            Logger.log("On VariableDeclarationExpr: [" + n + "]");
            n.getVariables()
                    .forEach(variableDeclarator -> {
                        variableDeclarator.getNameAsExpression().accept(this, action.or(VariableAction.Actions.DECLARATION)); // Declaration of the variable
                        variableDeclarator.getInitializer()
                                .ifPresent(expression -> {
                                    variableDeclarator.getNameAsExpression().accept(this, action.or(VariableAction.Actions.DEFINITION)); // Definition of the variable (it is initialized)
                                    expression.accept(this, action.or(VariableAction.Actions.USE)); // Use of the variables in the initialization
                                });
                    });
        }

        @Override
        public void visit(SwitchExpr n, VariableAction.Actions action) {
            Logger.log("On SwitchExpr: [" + n + "]");
            n.getSelector().accept(this, action.or(VariableAction.Actions.USE));
        }
    }
}
