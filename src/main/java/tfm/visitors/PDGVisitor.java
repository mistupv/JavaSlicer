package tfm.visitors;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGNode;
import tfm.utils.VariableExtractor;
import tfm.variables.actions.VariableAction;


public class PDGVisitor extends VoidVisitorAdapter<PDGNode> {

    private PDGGraph graph;

    public PDGVisitor(PDGGraph graph) {
        this.graph = graph;
    }

    @Override
    public void visit(ExpressionStmt n, PDGNode parent) {
        Expression expression = n.getExpression();

        PDGNode expressionNode = graph.addNode(expression.toString());

        graph.addControlDependencyArc(parent, expressionNode);

        VariableExtractor.Result result = VariableExtractor.extractFrom(expression);
        result.forEach((variable, actions) -> {
                    System.out.println(
                            String.format("Variable %s with actions on ExpressionStmt: %s",
                                    variable, actions
                            )
                    );

                    actions.forEach(action -> {
                        if (action == VariableAction.Actions.USE) {
                            graph.addVariableUse(variable, expressionNode);
                        } else {
                            graph.addVariableDefinition(variable, expressionNode);
                        }
                    });
                }
        );
    }

    @Override
    public void visit(IfStmt ifStmt, PDGNode parent) {
        PDGNode ifNode = graph.addNode(ifStmt.getCondition().toString());

        graph.addControlDependencyArc(parent, ifNode);

        VariableExtractor.Result result = VariableExtractor.extractFrom(ifStmt.getCondition());
        result.forEach((variable, actions) -> {
                    System.out.println(
                            String.format("Variable %s with actions on IfStmt: %s",
                                    variable, actions
                            )
                    );

                    actions.forEach(action -> {
                        if (action == VariableAction.Actions.DECLARATION) {
                            graph.addNewVariable(variable, ifNode);
                        }
                        if (action == VariableAction.Actions.DEFINITION) {
                            graph.addVariableDefinition(variable, ifNode);
                        } else {
                            if (graph.containsVariable(variable)) {
                                graph.addVariableUse(variable, ifNode);
                            }
                        }
                    });
                }
        );

        // Default adapter visits else before then, we have to visit then branch first
        ifStmt.getThenStmt().accept(this, ifNode);
        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, ifNode));
    }

    @Override
    public void visit(WhileStmt whileStmt, PDGNode parent) {
        PDGNode whileNode = graph.addNode(whileStmt.getCondition().toString());

        graph.addControlDependencyArc(parent, whileNode);

        VariableExtractor.Result result = VariableExtractor.extractFrom(whileStmt.getCondition());
        result.forEach((variable, actions) -> {
                System.out.println(
                        String.format("Variable %s with actions on WhileStmt: %s",
                                variable, actions
                        )
                );

                actions.forEach(action -> {
                    if (action == VariableAction.Actions.USE) {
                        graph.addVariableUse(variable, whileNode);
                    } else {
                        graph.addVariableDefinition(variable, whileNode);
                    }
                });
            }
        );

        super.visit(whileStmt, whileNode);
    }

    @Override
    public void visit(ForStmt forStmt, PDGNode parent) {
        // Add initialization nodes
        forStmt.getInitialization().stream()
                .map(expression -> graph.addNode(expression.toString()))
                .forEach(pdgVertex -> graph.addControlDependencyArc(parent, pdgVertex));

        // Add condition node
        Expression condition = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));
        PDGNode conditionNode = graph.addNode(condition.toString());

        graph.addControlDependencyArc(parent, conditionNode);

        // Visit for
        super.visit(forStmt, conditionNode);

        // Add update vertex
        forStmt.getUpdate().stream()
                .map(expression -> graph.addNode(expression.toString()))
                .forEach(pdgVertex -> graph.addControlDependencyArc(conditionNode, pdgVertex));
    }

    @Override
    public void visit(SwitchStmt switchStmt, PDGNode parent) {
        PDGNode switchNode = graph.addNode(switchStmt.toString());

        graph.addControlDependencyArc(parent, switchNode);

        switchStmt.getSelector().accept(this, parent);
        switchStmt.getEntries()
                .forEach(switchEntryStmt -> switchEntryStmt.accept(this, switchNode));
    }
}
