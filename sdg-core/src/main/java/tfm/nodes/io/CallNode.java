package tfm.nodes.io;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import tfm.nodes.SyntheticNode;

import java.util.LinkedList;

/** A node representing a call to a method or constructor. */
public class CallNode extends SyntheticNode<Node> {
    protected static final String LABEL_TEMPLATE = "CALL %s";

    public CallNode(MethodCallExpr astNode) {
        super(String.format(LABEL_TEMPLATE, astNode), astNode, new LinkedList<>());
    }

    public CallNode(ObjectCreationExpr astNode) {
        super(String.format(LABEL_TEMPLATE, astNode), astNode, new LinkedList<>());
    }

    public CallNode(ExplicitConstructorInvocationStmt astNode) {
        super(String.format(LABEL_TEMPLATE, astNode), astNode, new LinkedList<>());
    }

    @SuppressWarnings("unchecked")
    public Resolvable<? extends ResolvedMethodLikeDeclaration> getCallASTNode() {
        return (Resolvable<? extends ResolvedMethodLikeDeclaration>) astNode;
    }

    public static CallNode create(Resolvable<? extends ResolvedMethodLikeDeclaration> astNode) {
        return create((Object) astNode);
    }

    private static CallNode create(Object astNode) {
        if (astNode instanceof MethodCallExpr)
            return new CallNode((MethodCallExpr) astNode);
        else if (astNode instanceof ObjectCreationExpr)
            return new CallNode((ObjectCreationExpr) astNode);
        else if (astNode instanceof ExplicitConstructorInvocationStmt)
            return new CallNode((ExplicitConstructorInvocationStmt) astNode);
        else throw new IllegalArgumentException("Call node was not a proper type");
    }

    public static class Return extends SyntheticNode<Node> {
        protected static final String LABEL = "call return";

        public Return(MethodCallExpr astNode) {
            super(LABEL, astNode, new LinkedList<>());
        }

        public Return(ObjectCreationExpr astNode) {
            super(LABEL, astNode, new LinkedList<>());
        }

        public Return(ExplicitConstructorInvocationStmt astNode) {
            super(LABEL, astNode, new LinkedList<>());
        }

        public static Return create(Resolvable<? extends ResolvedMethodLikeDeclaration> astNode) {
            return create((Object) astNode);
        }

        private static Return create(Object astNode) {
            if (astNode instanceof MethodCallExpr)
                return new Return((MethodCallExpr) astNode);
            else if (astNode instanceof ObjectCreationExpr)
                return new Return((ObjectCreationExpr) astNode);
            else if (astNode instanceof ExplicitConstructorInvocationStmt)
                return new Return((ExplicitConstructorInvocationStmt) astNode);
            else throw new IllegalArgumentException("Call node was not a proper type");
        }
    }
}
