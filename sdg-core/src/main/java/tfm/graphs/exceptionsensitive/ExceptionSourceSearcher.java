package tfm.graphs.exceptionsensitive;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.resolution.types.ResolvedType;
import tfm.graphs.GraphNodeContentVisitor;

import java.util.Collection;
import java.util.Collections;

public class ExceptionSourceSearcher extends GraphNodeContentVisitor<Void> {
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
