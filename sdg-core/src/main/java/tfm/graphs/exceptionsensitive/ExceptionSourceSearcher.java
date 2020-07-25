package tfm.graphs.exceptionsensitive;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import tfm.graphs.GraphNodeContentVisitor;
import tfm.nodes.GraphNode;

import java.util.Collection;
import java.util.Collections;

/** A visitor that finds the first exception source in a given node.
 * @see #search(GraphNode) */
public class ExceptionSourceSearcher extends GraphNodeContentVisitor<Void> {
    /** Find the first exception source in a given graph node. */
    public Collection<ResolvedType> search(GraphNode<?> node) {
        try {
            node.getAstNode().accept(this, null);
        } catch (FoundException e) {
            return e.types;
        }
        return Collections.emptySet();
    }

    @Override
    public void visit(ThrowStmt n, Void arg) {
        throw new FoundException(n.getExpression().calculateResolvedType());
    }

    @Override
    public void visit(MethodCallExpr n, Void arg) {
        visitCall(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ObjectCreationExpr n, Void arg) {
        visitCall(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
        visitCall(n);
        super.visit(n, arg);
    }

    /** Check whether a call may throw exceptions or not. */
    public void visitCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        ResolvedMethodLikeDeclaration declaration = call.resolve();
        if (declaration.getNumberOfSpecifiedExceptions() > 0)
            throw new FoundException(declaration.getSpecifiedExceptions());
    }

    /** An exception to skip the call stack when the value is found. */
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
