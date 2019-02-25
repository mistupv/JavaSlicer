package tfm.graphlib.visitors;

import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.arcs.data.VoidArcData;
import tfm.graphlib.graphs.PDGGraph;
import tfm.graphlib.nodes.PDGVertex;

public class PDGVisitor extends VoidVisitorAdapter<PDGVertex> {

    private PDGGraph graph;

    public PDGVisitor(PDGGraph graph) {
        this.graph = graph;
    }

    @Override
    public void visit(ExpressionStmt n, PDGVertex arg) {
        PDGVertex node = graph.addVertex(n.getExpression().toString());

        graph.addControlDependencyArc(arg, node);

        super.visit(n, arg);
    }
}
