package tfm.graphlib.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import tfm.graphlib.arcs.Arc;
import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.arcs.data.VariableArcData;
import tfm.graphlib.arcs.data.VoidArcData;
import tfm.graphlib.arcs.pdg.ControlDependencyArc;
import tfm.graphlib.arcs.pdg.DataDependencyArc;
import tfm.graphlib.nodes.PDGVertex;

import java.util.*;

public abstract class PDGGraph extends Graph<PDGVertex> {

    private Map<String, List<VariableDeclarationExpr>> variablesDeclarations;
    private Map<String, List<VariableDeclarationExpr>> variablesUses;

    public PDGGraph() {
        setRootVertex(new PDGVertex(VertexId.getVertexId(), getRootNodeData()));

        variablesDeclarations = new HashMap<>();
        variablesUses = new HashMap<>();
    }

    protected abstract String getRootNodeData();

    @Override
    public PDGVertex addVertex(String instruction) {
        PDGVertex vertex = new PDGVertex(VertexId.getVertexId(), instruction);
        super.addVertex(vertex);

        return vertex;
    }

    @SuppressWarnings("unchecked")
    private void addArc(Arc arc) {
        super.addEdge(arc);
    }

    public void addControlDependencyArc(PDGVertex from, PDGVertex to) {
        ControlDependencyArc controlDependencyArc = new ControlDependencyArc(from, to);

        this.addArc(controlDependencyArc);
    }

    public void addDataDependencyArc(PDGVertex from, PDGVertex to, String variable) {
        DataDependencyArc dataDataDependencyArc = new DataDependencyArc(from, to, variable);

        this.addArc(dataDataDependencyArc);
    }

    public void addVariableDeclaration(String variable, VariableDeclarationExpr expr) {
        doAddVariableUseOrDeclaration(variable, expr, variablesDeclarations);
    }

    public void addVariableUse(String variable, VariableDeclarationExpr expr) {
        doAddVariableUseOrDeclaration(variable, expr, variablesUses);
    }

    private void doAddVariableUseOrDeclaration(String variable, VariableDeclarationExpr expr, Map<String, List<VariableDeclarationExpr>> map) {
        List<VariableDeclarationExpr> list = map.getOrDefault(variable, new ArrayList<>());
        list.add(expr);

        if (!map.containsKey(variable)) {
            map.put(variable, list);
        }
    }

    public List<VariableDeclarationExpr> getDeclarationsOf(String variable) {
        return variablesDeclarations.get(variable);
    }

    public List<VariableDeclarationExpr> getUsesOf(String variable) {
        return variablesUses.get(variable);
    }
}
