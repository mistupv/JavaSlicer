package tfm.graphs.exceptionsensitive;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.Collection;
import java.util.Collections;

public class ExceptionSourceSearcher extends VoidVisitorAdapter<Void> {
    public Collection<ResolvedType> search(Node node) {
        try {
            node.accept(this, null);
            return Collections.emptySet();
        } catch (FoundException e) {
            return e.types;
        }
    }

    @Override
    public void visit(ThrowStmt n, Void arg) {
        throw new FoundException(n.getExpression().calculateResolvedType());
    }

    @Override
    public void visit(MethodCallExpr n, Void arg) {
        if (n.resolve().getNumberOfSpecifiedExceptions() > 0)
            throw new FoundException(n.resolve().getSpecifiedExceptions());
        else super.visit(n, arg);
    }

    // The following are completely traversed (they contain no statements):
    // AssertStmt, BreakStmt, ContinueStmt, EmptyStmt, ExplicitConstructorInvocationStmt,
    // ReturnStmt, ExpressionStmt, LocalClassDeclarationStmt

    // The following are not traversed at all: they only contain statements
    @Override
    public void visit(BlockStmt n, Void arg) {}

    @Override
    public void visit(SwitchEntryStmt n, Void arg) {}

    @Override
    public void visit(TryStmt n, Void arg) {}

    @Override
    public void visit(LabeledStmt n, Void arg) {}

    // The following are partially traversed (expressions are traversed, statements are not)
    @Override
    public void visit(DoStmt n, Void arg) {
        n.getCondition().accept(this, arg);
    }

    @Override
    public void visit(ForEachStmt n, Void arg) {
        n.getIterable().accept(this, arg);
    }

    @Override
    public void visit(ForStmt n, Void arg) {
        n.getCompare().ifPresent(comp -> comp.accept(this, arg));
    }

    @Override
    public void visit(IfStmt n, Void arg) {
        n.getCondition().accept(this, arg);
    }

    @Override
    public void visit(SwitchStmt n, Void arg) {
        n.getSelector().accept(this, arg);
    }

    @Override
    public void visit(WhileStmt n, Void arg) {
        n.getCondition().accept(this, arg);
    }

    static class FoundException extends RuntimeException {
        protected final Collection<ResolvedType> types;

        public FoundException(ResolvedType type) {
            this(Collections.singleton(type));
        }

        public FoundException(Collection<ResolvedType> types) {
            this.types = types;
        }
    }
}
