package tfm.nodes.exceptionsensitive;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;

/** A node that represents the times when the call performed returns without an exception. */
public class NormalReturnNode extends ReturnNode {
    public NormalReturnNode(MethodCallExpr astNode) {
        super("normal return", astNode);
    }

    public NormalReturnNode(ObjectCreationExpr astNode) {
        super("normal return", astNode);
    }

    public NormalReturnNode(ExplicitConstructorInvocationStmt astNode) {
        super("normal return", astNode);
    }

    public static NormalReturnNode create(Resolvable<? extends ResolvedMethodLikeDeclaration> astNode) {
        return create((Object) astNode);
    }

    private static NormalReturnNode create(Object astNode) {
        if (astNode instanceof MethodCallExpr)
            return new NormalReturnNode((MethodCallExpr) astNode);
        else if (astNode instanceof ObjectCreationExpr)
            return new NormalReturnNode((ObjectCreationExpr) astNode);
        else if (astNode instanceof ExplicitConstructorInvocationStmt)
            return new NormalReturnNode((ExplicitConstructorInvocationStmt) astNode);
        else throw new IllegalArgumentException("Call node was not a proper type");
    }
}
