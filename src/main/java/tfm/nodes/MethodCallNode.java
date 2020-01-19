package tfm.nodes;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MethodCallNode extends GraphNode<ExpressionStmt> {

    private List<GraphNode> inParameters;
    private List<GraphNode> outParameters;

    public <N1 extends GraphNode<ExpressionStmt>> MethodCallNode(N1 node) {
        super(node);

        this.inParameters = new ArrayList<>();
        this.outParameters = new ArrayList<>();
    }

    public MethodCallNode(int id, String representation, ExpressionStmt node) {
        super(id, representation, node);

        this.inParameters = new ArrayList<>();
        this.outParameters = new ArrayList<>();
    }

    public MethodCallNode(int id, String representation, @NonNull ExpressionStmt node, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        super(id, representation, node, declaredVariables, definedVariables, usedVariables);

        this.inParameters = new ArrayList<>();
        this.outParameters = new ArrayList<>();
    }

    public List<GraphNode> getOutParameters() {
        return outParameters;
    }

    public void setOutParameters(List<GraphNode> outParameters) {
        this.outParameters = outParameters;
    }

    public List<GraphNode> getInParameters() {
        return inParameters;
    }

    public void setInParameters(List<GraphNode> inParameters) {
        this.inParameters = inParameters;
    }
}
