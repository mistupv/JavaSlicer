package tfm.graphlib.visitors;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphlib.graphs.PDGGraph;
import tfm.graphlib.nodes.PDGVertex;

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
}
