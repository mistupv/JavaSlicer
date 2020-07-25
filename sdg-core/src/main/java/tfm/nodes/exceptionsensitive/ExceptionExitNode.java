package tfm.nodes.exceptionsensitive;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.Objects;

/** A node that summarizes the exceptions that may reach the end of a declaration. */
public class ExceptionExitNode extends ExitNode {
    protected final ResolvedType exceptionType;

    public ExceptionExitNode(CallableDeclaration<?> astNode, ResolvedType exceptionType) {
        super(exceptionType.describe() + " exit", astNode);
        this.exceptionType = Objects.requireNonNull(exceptionType);
    }

    public ResolvedType getExceptionType() {
        return exceptionType;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof ExceptionExitNode &&
                exceptionType.equals(((ExceptionExitNode) o).exceptionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), exceptionType);
    }
}
