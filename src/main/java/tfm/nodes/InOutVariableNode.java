package tfm.nodes;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class InOutVariableNode extends GraphNode<ExpressionStmt> {

    public InOutVariableNode(int id, String instruction, @NotNull ExpressionStmt astNode) {
        super(id, instruction, astNode);
    }

    public InOutVariableNode(int id, String instruction, @NotNull ExpressionStmt astNode, Collection<String> declaredVariables, Collection<String> definedVariables, Collection<String> usedVariables) {
        super(id, instruction, astNode, declaredVariables, definedVariables, usedVariables);
    }

    public String getDeclaredVariable() {
        return getDeclaredVariables().iterator().next();
    }

    public String getUsedVariable() {
        return getUsedVariables().iterator().next();
    }
}
