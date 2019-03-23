package tfm.visitors;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGVertex;

import java.util.List;
import java.util.stream.Collectors;

public class PDGVisitor extends VoidVisitorAdapter<PDGVertex> {

    private PDGGraph graph;

    public PDGVisitor(PDGGraph graph) {
        this.graph = graph;
    }

    @Override
    public void visit(ExpressionStmt n, PDGVertex parent) {
        PDGVertex expressionNode = graph.addVertex(n.getExpression().toString());

        graph.addControlDependencyArc(parent, expressionNode);

        n.getExpression().ifAssignExpr(assignExpr -> {
            assignExpr.getTarget().ifVariableDeclarationExpr(variableDeclarationExpr -> {
                variableDeclarationExpr.getVariables().forEach(variableDeclarator -> {
                    String name = variableDeclarator.getName().asString();
                });
            });
        });

        super.visit(n, parent);
    }

    @Override
    public void visit(IfStmt ifStmt, PDGVertex parent) {
        PDGVertex ifNode = graph.addVertex(ifStmt.getCondition().toString());

        graph.addControlDependencyArc(parent, ifNode);

        super.visit(ifStmt, ifNode);
    }

    @Override
    public void visit(WhileStmt whileStmt, PDGVertex parent) {
        PDGVertex whileNode = graph.addVertex(whileStmt.getCondition().toString());

        graph.addControlDependencyArc(parent, whileNode);

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
}
