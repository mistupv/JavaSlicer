package tfm.nodes.exceptionsensitive;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.Objects;

/** A node that represents the times when the call performed returns with an exception. */
public class ExceptionReturnNode extends ReturnNode {
    protected ResolvedType exceptionType;

    public ExceptionReturnNode(MethodCallExpr astNode, ResolvedType exceptionType) {
        super(exceptionType.describe() + " return", astNode);
        this.exceptionType = Objects.requireNonNull(exceptionType);
    }

    public ExceptionReturnNode(ObjectCreationExpr astNode, ResolvedType exceptionType) {
        super(exceptionType.describe() + " return", astNode);
        this.exceptionType = Objects.requireNonNull(exceptionType);
    }

    public ExceptionReturnNode(ExplicitConstructorInvocationStmt astNode, ResolvedType exceptionType) {
        super(exceptionType.describe() + " return", astNode);
        this.exceptionType = Objects.requireNonNull(exceptionType);
    }

    public ResolvedType getExceptionType() {
        return exceptionType;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof ExceptionReturnNode &&
                exceptionType.equals(((ExceptionReturnNode) o).exceptionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), exceptionType);
    }

    public static ExceptionReturnNode create(Resolvable<? extends ResolvedMethodLikeDeclaration> astNode, ResolvedType rType) {
        return create((Object) astNode, rType);
    }

    private static ExceptionReturnNode create(Object astNode, ResolvedType rType) {
        if (astNode instanceof MethodCallExpr)
            return new ExceptionReturnNode((MethodCallExpr) astNode, rType);
        else if (astNode instanceof ObjectCreationExpr)
            return new ExceptionReturnNode((ObjectCreationExpr) astNode, rType);
        else if (astNode instanceof ExplicitConstructorInvocationStmt)
            return new ExceptionReturnNode((ExplicitConstructorInvocationStmt) astNode, rType);
        else throw new IllegalArgumentException("Call node was not a proper type");
    }
}
