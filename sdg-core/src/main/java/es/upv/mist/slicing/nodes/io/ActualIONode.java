package es.upv.mist.slicing.nodes.io;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.Objects;

/** A node that represents actual-in and actual-out nodes in a call. */
public class ActualIONode extends IONode<Node> {
    protected final Expression argument;

    protected ActualIONode(Resolvable<? extends ResolvedMethodLikeDeclaration> astNode, String variable, Expression argument, boolean isInput) {
        super(createLabel(isInput, variable, argument), (Node) astNode, variable, isInput);
        this.argument = argument;
    }

    public Expression getArgument() {
        return argument;
    }

    public boolean matchesFormalIO(FormalIONode o) {
        // 1. We must be an ActualIONode, o must be a FormalIONode
        return getClass().equals(ActualIONode.class) && o.getClass().equals(FormalIONode.class)
                // 2. Our variables must match (type + name)
                && Objects.equals(variableName, o.variableName)
                // 3. in matches in, out matches out
                && isInput() == o.isInput()
                // 4. The method call must resolve to the method declaration of the argument.
                && Objects.equals(o.getAstNode(), resolvedASTNode());
    }

    @SuppressWarnings("unchecked")
    protected BodyDeclaration<?> resolvedASTNode() {
        return ASTUtils.getResolvedAST(((Resolvable<? extends ResolvedMethodLikeDeclaration>) astNode).resolve())
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof ActualIONode
                && Objects.equals(((ActualIONode) o).argument, argument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), argument);
    }

    protected static String createLabel(boolean isInput, String paramName, Expression arg) {
        if (isInput)
            return String.format("%s_in = %s", paramName, arg);
        else
            return String.format("%s = %s_out", arg, paramName);
    }

    public static ActualIONode createActualIn(Resolvable<? extends ResolvedMethodLikeDeclaration> methodCallExpr, String name, Expression argument) {
        return new ActualIONode(methodCallExpr, name, argument, true);
    }

    public static ActualIONode createActualOut(Resolvable<? extends ResolvedMethodLikeDeclaration> methodCallExpr, String name, Expression argument) {
        return new ActualIONode(methodCallExpr, name, argument, false);
    }
}
