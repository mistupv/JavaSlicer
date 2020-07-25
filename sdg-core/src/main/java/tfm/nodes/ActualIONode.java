package tfm.nodes;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import tfm.nodes.type.NodeType;

import java.util.Objects;
import java.util.Set;

import static tfm.nodes.type.NodeType.*;

public class ActualIONode extends IONode<MethodCallExpr> {
    protected final static Set<NodeType> VALID_NODE_TYPES = Set.of(ACTUAL_IN, ACTUAL_OUT);
    protected Expression argument;

    protected ActualIONode(NodeType type, MethodCallExpr astNode, Parameter parameter, Expression argument) {
        super(type, createLabel(type, parameter, argument), astNode, parameter);
        if (!VALID_NODE_TYPES.contains(type))
            throw new IllegalArgumentException("Illegal type for actual-in/out node");
        this.argument = Objects.requireNonNull(argument);
    }

    public Expression getArgument() {
        return argument;
    }

    public boolean matchesFormalIO(FormalIONode o) {
        // 1. We must be an ActualIONode, o must be a FormalIONode
        return getClass().equals(ActualIONode.class) && o.getClass().equals(FormalIONode.class)
                // 2. Our variables must match (type + name)
                && parameter.equals(o.parameter)
                // 3a. If ACTUAL_IN, the arg must be FORMAL_IN
                && ((nodeType.equals(ACTUAL_IN) && o.nodeType.equals(FORMAL_IN))
                    // 3b. same for ACTUAL_OUT--FORMAL_OUT
                    || (nodeType.equals(ACTUAL_OUT) && o.nodeType.equals(FORMAL_OUT)))
                // 4. The method call must resolve to the method declaration of the argument.
                && Objects.equals(o.astNode, astNode.resolve().toAst().orElse(null));
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

    protected static String createLabel(NodeType type, Parameter param, Expression arg) {
        switch (type) {
            case ACTUAL_IN:
                return String.format("%s %s_in = %s", param.getTypeAsString(), param.getNameAsString(), arg.toString());
            case ACTUAL_OUT:
                return String.format("%s = %s_out", arg, param.getNameAsString());
            default:
                throw new IllegalStateException("Invalid NodeType for actual-in/out node: " + type);
        }
    }

    public static ActualIONode createActualIn(MethodCallExpr methodCallExpr, Parameter parameter, Expression argument) {
        return new ActualIONode(ACTUAL_IN, methodCallExpr, parameter, argument);
    }

    public static ActualIONode createActualOut(MethodCallExpr methodCallExpr, Parameter parameter, Expression argument) {
        return new ActualIONode(ACTUAL_OUT, methodCallExpr, parameter, argument);
    }
}
