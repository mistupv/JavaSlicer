package tfm.visitors;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGVertex;
import tfm.utils.VariableExtractor;
import tfm.variables.actions.VariableAction;

import java.util.List;
import java.util.Map;

public class PDGVisitor extends VoidVisitorAdapter<PDGVertex> {

    private PDGGraph graph;

    public PDGVisitor(PDGGraph graph) {
        this.graph = graph;
    }

    @Override
    public void visit(ExpressionStmt n, PDGVertex parent) {
        Expression expression = n.getExpression();

        PDGVertex expressionNode = graph.addVertex(expression.toString());

        graph.addControlDependencyArc(parent, expressionNode);

        VariableExtractor.Result result = VariableExtractor.parse(expression);
        result.variableActions.forEach((variable, actions) -> {
                    System.out.println(
                            String.format("Variable %s with actions on ExpressionStmt: %s",
                                    variable, actions
                            )
                    );

                    actions.forEach(action -> {
                        if (action == VariableAction.Actions.READ) {
                            graph.addVariableRead(variable, expressionNode);
                        } else {
                            graph.addVariableWrite(variable, expressionNode);
                        }
                    });
                }
        );
    }

    @Override
    public void visit(IfStmt ifStmt, PDGVertex parent) {
        PDGVertex ifNode = graph.addVertex(ifStmt.getCondition().toString());

        graph.addControlDependencyArc(parent, ifNode);

        VariableExtractor.Result result = VariableExtractor.parse(ifStmt.getCondition());
        result.variableActions.forEach((variable, actions) -> {
                    System.out.println(
                            String.format("Variable %s with actions on IfStmt: %s",
                                    variable, actions
                            )
                    );

                    actions.forEach(action -> {
                        if (action == VariableAction.Actions.READ) {
                            graph.addVariableRead(variable, ifNode);
                        } else {
                            graph.addVariableWrite(variable, ifNode);
                        }
                    });
                }
        );

        // Default adapter visits else before then, we have to visit then branch first
        ifStmt.getThenStmt().accept(this, ifNode);
        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, ifNode));
    }

    @Override
    public void visit(WhileStmt whileStmt, PDGVertex parent) {
        PDGVertex whileNode = graph.addVertex(whileStmt.getCondition().toString());

        graph.addControlDependencyArc(parent, whileNode);

        VariableExtractor.Result result = VariableExtractor.parse(whileStmt.getCondition());
        result.variableActions.forEach((variable, actions) -> {
                System.out.println(
                        String.format("Variable %s with actions on WhileStmt: %s",
                                variable, actions
                        )
                );

                actions.forEach(action -> {
                    if (action == VariableAction.Actions.READ) {
                        graph.addVariableRead(variable, whileNode);
                    } else {
                        graph.addVariableWrite(variable, whileNode);
                    }
                });
            }
        );

        super.visit(whileStmt, whileNode);
    }

    @Override
    public void visit(ForStmt forStmt, PDGVertex parent) {
        // Add initialization nodes
        forStmt.getInitialization().stream()
                .map(expression -> graph.addVertex(expression.toString()))
                .forEach(pdgVertex -> graph.addControlDependencyArc(parent, pdgVertex));

        // Add condition node
        Expression condition = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));
        PDGVertex conditionNode = graph.addVertex(condition.toString());

        graph.addControlDependencyArc(parent, conditionNode);

        // Visit for
        super.visit(forStmt, conditionNode);

        // Add update vertex
        forStmt.getUpdate().stream()
                .map(expression -> graph.addVertex(expression.toString()))
                .forEach(pdgVertex -> graph.addControlDependencyArc(conditionNode, pdgVertex));
    }

    @Override
    public void visit(SwitchStmt switchStmt, PDGVertex parent) {
        PDGVertex switchNode = graph.addVertex(switchStmt.toString());

        graph.addControlDependencyArc(parent, switchNode);

        switchStmt.getSelector().accept(this, parent);
        switchStmt.getEntries()
                .forEach(switchEntryStmt -> switchEntryStmt.accept(this, switchNode));
    }
}
