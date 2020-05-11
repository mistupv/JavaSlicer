package tfm.graphs.sdg;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.utils.Logger;

import java.util.Set;

public class OutNodeVariableVisitor extends VoidVisitorAdapter<Set<String>> {

    @Override
    public void visit(ArrayAccessExpr n, Set<String> variables) {
        n.getName().accept(this, variables);
    }

    @Override
    public void visit(CastExpr n, Set<String> variables) {
        n.getExpression().accept(this, variables);
    }

    @Override
    public void visit(ConditionalExpr n, Set<String> variables) {
        n.getThenExpr().accept(this, variables);
        n.getElseExpr().accept(this, variables);
    }

    @Override
    public void visit(EnclosedExpr n, Set<String> variables) {
        n.getInner().accept(this, variables);
    }

    @Override
    public void visit(ExpressionStmt n, Set<String> variables) {
        n.getExpression().accept(this, variables);
    }

    @Override
    public void visit(FieldAccessExpr n, Set<String> variables) {
        Logger.log("ShouldHaveOutNodeVisitor", "Exploring " + n);
        n.getScope().accept(this, variables);
    }

    @Override
    public void visit(NameExpr n, Set<String> variables) {
        Logger.log("ShouldHaveOutNodeVisitor", n + " is a variable!!");
        variables.add(n.getNameAsString());
    }

    @Override
    public void visit(UnaryExpr n, Set<String> variables) {
        switch (n.getOperator()) {
            case PLUS:
            case MINUS:
            case BITWISE_COMPLEMENT:
            case LOGICAL_COMPLEMENT:
                ;
        }

        n.getExpression().accept(this, variables);
    }
}
