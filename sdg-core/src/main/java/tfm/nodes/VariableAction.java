package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.NameExpr;
import tfm.utils.ASTUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/** An action upon a variable (e.g. usage, definition) */
public abstract class VariableAction {
    protected final NameExpr variable;
    protected final GraphNode<?> graphNode;

    protected boolean optional = false;

    public VariableAction(NameExpr variable, GraphNode<?> graphNode) {
        this.variable = variable;
        this.graphNode = graphNode;
    }

    public VariableAction moveTo(GraphNode<?> destination) {
        getGraphNode().variableActions.remove(this);
        try {
            VariableAction a = getClass().getConstructor(NameExpr.class, GraphNode.class).newInstance(variable, destination);
            destination.variableActions.add(a);
            return a;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new UnsupportedOperationException("The VariableAction constructor has changed!", e);
        }
    }

    public String getVariable() {
        return variable.getNameAsString();
    }

    public boolean isOptional() {
        return optional;
    }

    public GraphNode<?> getGraphNode() {
        return graphNode;
    }

    public boolean matches(VariableAction action) {
        return Objects.equals(action.variable, variable);
    }

    public boolean isContainedIn(Node node) {
        Node me = variable;
        while (me != null) {
            if (ASTUtils.equalsWithRangeInCU(node, me))
                return true;
            me = me.getParentNode().orElse(null);
        }
        return false;
    }

    public final boolean isUsage() {
        return this instanceof Usage;
    }

    public final boolean isDefinition() {
        return this instanceof Definition;
    }

    public final boolean isDeclaration() {
        return this instanceof Declaration;
    }

    @Override
    public String toString() {
        return "{" + variable + "}";
    }

    public static class Usage extends VariableAction {
        public Usage(NameExpr variable, GraphNode<?> graphNode) {
            super(variable, graphNode);
        }

        @Override
        public String toString() {
            return "USE" + super.toString();
        }
    }

    public static class Definition extends VariableAction {
        public Definition(NameExpr variable, GraphNode<?> graphNode) {
            super(variable, graphNode);
        }

        @Override
        public String toString() {
            return "DEF" + super.toString();
        }
    }

    public static class Declaration extends VariableAction {
        public Declaration(NameExpr variable, GraphNode<?> graphNode) {
            super(variable, graphNode);
        }

        @Override
        public String toString() {
            return "DEC" + super.toString();
        }
    }
}
