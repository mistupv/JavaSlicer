package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Set;

/** Visitor that obtains a set of variables that may have been redefined in
 *  an expression passed as parameter to a call. */
public class OutNodeVariableVisitor extends VoidVisitorAdapter<Set<Expression>> {

    @Override
    public void visit(ArrayAccessExpr n, Set<Expression> variables) {
        n.getName().accept(this, variables);
    }

    @Override
    public void visit(CastExpr n, Set<Expression> variables) {
        n.getExpression().accept(this, variables);
    }

    @Override
    public void visit(ConditionalExpr n, Set<Expression> variables) {
        n.getThenExpr().accept(this, variables);
        n.getElseExpr().accept(this, variables);
    }

    @Override
    public void visit(EnclosedExpr n, Set<Expression> variables) {
        n.getInner().accept(this, variables);
    }

    @Override
    public void visit(ExpressionStmt n, Set<Expression> variables) {
        n.getExpression().accept(this, variables);
    }

    @Override
    public void visit(FieldAccessExpr n, Set<Expression> variables) {
        n.getScope().accept(this, variables);
    }

    @Override
    public void visit(ThisExpr n, Set<Expression> variables) {
        variables.add(n);
    }

    @Override
    public void visit(NameExpr n, Set<Expression> variables) {
        variables.add(n);
    }

    // Expressions that stop the visit: no object can be outputted, modified inside the call and returned so that
    // we may access its value.

    @Override
    public void visit(UnaryExpr n, Set<Expression> variables) {}

    @Override
    public void visit(ArrayCreationExpr n, Set<Expression> arg) {}

    @Override
    public void visit(ArrayInitializerExpr n, Set<Expression> arg) {}

    @Override
    public void visit(BinaryExpr n, Set<Expression> arg) {}

    @Override
    public void visit(ClassExpr n, Set<Expression> arg) {}

    @Override
    public void visit(InstanceOfExpr n, Set<Expression> arg) {}

    @Override
    public void visit(MethodCallExpr n, Set<Expression> arg) {}

    @Override
    public void visit(ObjectCreationExpr n, Set<Expression> arg) {}

    @Override
    public void visit(LambdaExpr n, Set<Expression> arg) {}

    @Override
    public void visit(MethodReferenceExpr n, Set<Expression> arg) {}
}
