package tfm.nodes;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import edg.graphlib.Arrow;
import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.arcs.data.ArcData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class MethodCallNode extends SDGNode<ExpressionStmt> {

    private List<AuxiliarSDGNode> inParameters;
    private List<AuxiliarSDGNode> outParameters;

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

    public MethodCallNode(int id, String representation, @NonNull ExpressionStmt node, Collection<? extends Arrow<String, ArcData>> incomingArcs, Collection<? extends Arrow<String, ArcData>> outgoingArcs, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        super(id, representation, node, incomingArcs, outgoingArcs, declaredVariables, definedVariables, usedVariables);

        this.inParameters = new ArrayList<>();
        this.outParameters = new ArrayList<>();
    }

    public List<AuxiliarSDGNode> getOutParameters() {
        return outParameters;
    }

    public void setOutParameters(List<AuxiliarSDGNode> outParameters) {
        this.outParameters = outParameters;
    }

    public List<AuxiliarSDGNode> getInParameters() {
        return inParameters;
    }

    public void setInParameters(List<AuxiliarSDGNode> inParameters) {
        this.inParameters = inParameters;
    }
}
