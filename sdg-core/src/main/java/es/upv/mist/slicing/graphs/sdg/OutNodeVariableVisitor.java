package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Set;

/** Visitor that obtains a set of variables that may have been redefined in
 *  an expression passed as parameter to a call. */
public class OutNodeVariableVisitor extends VoidVisitorAdapter<Set<NameExpr>> {

    @Override
    public void visit(ArrayAccessExpr n, Set<NameExpr> variables) {
        n.getName().accept(this, variables);
    }

    @Override
    public void visit(CastExpr n, Set<NameExpr> variables) {
        n.getExpression().accept(this, variables);
    }

    @Override
    public void visit(ConditionalExpr n, Set<NameExpr> variables) {
        n.getThenExpr().accept(this, variables);
        n.getElseExpr().accept(this, variables);
    }

    @Override
    public void visit(EnclosedExpr n, Set<NameExpr> variables) {
        n.getInner().accept(this, variables);
    }

    @Override
    public void visit(ExpressionStmt n, Set<NameExpr> variables) {
        n.getExpression().accept(this, variables);
    }

    @Override
    public void visit(FieldAccessExpr n, Set<NameExpr> variables) {
        n.getScope().accept(this, variables);
    }

    @Override
    public void visit(NameExpr n, Set<NameExpr> variables) {
        variables.add(n);
    }

    @Override
    public void visit(UnaryExpr n, Set<NameExpr> variables) {
        switch (n.getOperator()) {
            case POSTFIX_DECREMENT:
            case POSTFIX_INCREMENT:
            case PREFIX_DECREMENT:
            case PREFIX_INCREMENT:
                n.getExpression().accept(this, variables);
        }
    }
}
