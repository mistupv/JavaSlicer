package tfm.nodes;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import tfm.nodes.type.NodeType;

import java.util.Objects;

public class ExceptionReturnNode extends ReturnNode {
    protected ResolvedType exceptionType;

    public ExceptionReturnNode(MethodCallExpr astNode, ResolvedType exceptionType) {
        super(NodeType.METHOD_CALL_EXCEPTION_RETURN, exceptionType.describe() + " return", astNode);
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
}
