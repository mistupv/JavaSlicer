package tfm.nodes;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;

public class MethodCallNode extends NodeWithInOutVariables<ExpressionStmt> {

    public MethodCallNode(int id, String representation, ExpressionStmt node) {
        super(id, representation, node);
    }

    public MethodCallNode(int id, String representation, @NonNull ExpressionStmt node, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        super(id, representation, node, declaredVariables, definedVariables, usedVariables);
    }
}
