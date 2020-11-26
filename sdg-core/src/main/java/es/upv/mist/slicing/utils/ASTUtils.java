package es.upv.mist.slicing.utils;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JavaParser-related utility functions. */
public class ASTUtils {
    private ASTUtils() {
        throw new UnsupportedOperationException("This is a static, utility class");
    }

    public static boolean isContained(Node upper, Node contained) {
        Optional<Node> parent = contained.getParentNode();
        if (parent.isEmpty())
            return false;
        return equalsWithRangeInCU(upper, parent.get()) || isContained(upper, parent.get());
    }

    public static boolean switchHasDefaultCase(SwitchStmt stmt) {
        return switchGetDefaultCase(stmt) != null;
    }

    public static SwitchEntry switchGetDefaultCase(SwitchStmt stmt) {
        for (SwitchEntry entry : stmt.getEntries())
            if (entry.getLabels().isEmpty())
                return entry;
        return null;
    }

    public static boolean equalsWithRange(Node n1, Node n2) {
        return Objects.equals(n1.getRange(), n2.getRange()) && Objects.equals(n1, n2);
    }

    public static boolean equalsWithRangeInCU(Node n1, Node n2) {
        return n1.findCompilationUnit().equals(n2.findCompilationUnit())
                && equalsWithRange(n1, n2);
    }

    public static boolean resolvableIsVoid(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        var resolved = call.resolve();
        if (resolved instanceof ResolvedMethodDeclaration)
            return ((ResolvedMethodDeclaration) resolved).getReturnType().isVoid();
        if (resolved instanceof ResolvedConstructorDeclaration)
            return false;
        throw new IllegalArgumentException("Call didn't resolve to either method or constructor!");
    }

    public static int getMatchingParameterIndex(CallableDeclaration<?> declaration, ResolvedParameterDeclaration param) {
        var parameters = declaration.getParameters();
        for (int i = 0; i < parameters.size(); i++)
            if (resolvedParameterEquals(param, parameters.get(i).resolve()))
                return i;
        throw new IllegalArgumentException("Expression resolved to a parameter, but could not be found!");
    }

    public static int getMatchingParameterIndex(ResolvedMethodLikeDeclaration declaration, ResolvedParameterDeclaration param) {
        for (int i = 0; i < declaration.getNumberOfParams(); i++)
            if (resolvedParameterEquals(declaration.getParam(i), param))
                return i;
        throw new IllegalArgumentException("Expression resolved to a parameter, but could not be found!");
    }

    protected static boolean resolvedParameterEquals(ResolvedParameterDeclaration p1, ResolvedParameterDeclaration p2) {
        return p2.getType().equals(p1.getType()) && p2.getName().equals(p1.getName());
    }

    public static List<Expression> getResolvableArgs(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        if (call instanceof MethodCallExpr)
            return ((MethodCallExpr) call).getArguments();
        if (call instanceof ObjectCreationExpr)
            return ((ObjectCreationExpr) call).getArguments();
        if (call instanceof ExplicitConstructorInvocationStmt)
            return ((ExplicitConstructorInvocationStmt) call).getArguments();
        throw new IllegalArgumentException("Call wasn't of a compatible type!");
    }

    public static BlockStmt getCallableBody(CallableDeclaration<?> callableDeclaration) {
        if (callableDeclaration instanceof MethodDeclaration)
            return ((MethodDeclaration) callableDeclaration).getBody().orElseThrow(() -> new IllegalStateException("Graph creation is not allowed for abstract or native methods!"));
        if (callableDeclaration instanceof ConstructorDeclaration)
            return ((ConstructorDeclaration) callableDeclaration).getBody();
        throw new IllegalStateException("The method must have a body!");
    }

    public static Optional<Expression> getResolvableScope(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        if (call instanceof MethodCallExpr)
            return ((MethodCallExpr) call).getScope();
        if (call instanceof ObjectCreationExpr)
            return ((ObjectCreationExpr) call).getScope();
        if (call instanceof ExplicitConstructorInvocationStmt)
            return Optional.empty();
        throw new IllegalArgumentException("Call wasn't of a compatible type!");
    }

    public static Optional<? extends CallableDeclaration<?>> getResolvedAST(ResolvedMethodLikeDeclaration resolvedDeclaration) {
        if (resolvedDeclaration instanceof ResolvedMethodDeclaration)
            return ((ResolvedMethodDeclaration) resolvedDeclaration).toAst();
        if (resolvedDeclaration instanceof ResolvedConstructorDeclaration)
            return ((ResolvedConstructorDeclaration) resolvedDeclaration).toAst();
        throw new IllegalStateException("AST node of invalid type");
    }
}
