package tfm.graphs.exceptionsensitive;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import tfm.graphs.sdg.MethodCallReplacerVisitor;
import tfm.nodes.*;
import tfm.utils.Context;

import java.util.Set;
import java.util.stream.Collectors;

public class ExceptionSensitiveMethodCallReplacerVisitor extends MethodCallReplacerVisitor {
    public ExceptionSensitiveMethodCallReplacerVisitor(ESSDG sdg) {
        super(sdg);
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Context context) {
        if (methodCallExpr.resolve().getNumberOfSpecifiedExceptions() > 0)
            connectExitToReturn(methodCallExpr);
        super.visit(methodCallExpr, context);
    }

    protected void connectExitToReturn(MethodCallExpr call) {
        Set<SyntheticNode<?>> synthNodes = sdg.vertexSet().stream()
                .filter(SyntheticNode.class::isInstance)
                .map(n -> (SyntheticNode<?>) n)
                .collect(Collectors.toSet());
        ResolvedMethodDeclaration resolvedDecl = call.resolve();
        MethodDeclaration decl = resolvedDecl.toAst().orElseThrow();

        ReturnNode normalReturn = (ReturnNode) synthNodes.stream()
                .filter(NormalReturnNode.class::isInstance)
                .filter(n -> n.getAstNode() == call)
                .findAny().orElseThrow();
        ExitNode normalExit = (ExitNode) synthNodes.stream()
                .filter(NormalExitNode.class::isInstance)
                .filter(n -> n.getAstNode() == decl)
                .findAny().orElseThrow();
        ((ESSDG) sdg).addReturnArc(normalExit, normalReturn);

        for (ResolvedType type : resolvedDecl.getSpecifiedExceptions()) {
            ReturnNode exceptionReturn = synthNodes.stream()
                    .filter(ExceptionReturnNode.class::isInstance)
                    .map(ExceptionReturnNode.class::cast)
                    .filter(n -> n.getAstNode() == call)
                    .filter(n -> n.getExceptionType().equals(type))
                    .findAny().orElseThrow();
            ExitNode exceptionExit = synthNodes.stream()
                    .filter(ExceptionExitNode.class::isInstance)
                    .map(ExceptionExitNode.class::cast)
                    .filter(n -> n.getAstNode() == decl)
                    .filter(n -> n.getExceptionType().equals(type))
                    .findAny().orElseThrow();
            ((ESSDG) sdg).addReturnArc(exceptionExit, exceptionReturn);
        }
    }
}
