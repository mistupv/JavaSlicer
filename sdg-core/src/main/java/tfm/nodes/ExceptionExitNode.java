package tfm.nodes;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import tfm.nodes.type.NodeType;

import java.util.Objects;

public class ExceptionExitNode extends ExitNode {
    protected final ResolvedType exceptionType;

    public ExceptionExitNode(MethodDeclaration astNode, ResolvedType exceptionType) {
        super(NodeType.METHOD_EXCEPTION_EXIT, exceptionType.describe() + " exit", astNode);
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
