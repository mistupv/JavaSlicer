package tfm.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;

public class PDGCFGVisitor extends VoidVisitorAdapter<GraphNode<?>> {

    private PDGGraph pdgGraph;
    private CFGGraph cfgGraph;

    public PDGCFGVisitor(PDGGraph pdgGraph) {
        this(pdgGraph, new CFGGraph() {
            @Override
            protected String getRootNodeData() {
                return "Start";
            }
        });
    }

    public PDGCFGVisitor(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;

        this.pdgGraph.setCfgGraph(cfgGraph);
    }

    public void visit(MethodDeclaration methodDeclaration, GraphNode<?> parent) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        BlockStmt methodBody = methodDeclaration.getBody().get();

        // build CFG
        methodBody.accept(new CFGVisitor(cfgGraph), null);

        // Build control dependency
        ControlDependencyVisitor controlDependencyVisitor = new ControlDependencyVisitor(pdgGraph, cfgGraph);
        methodBody.accept(controlDependencyVisitor, parent);

        // Build data dependency
        DataDependencyVisitor dataDependencyVisitor = new DataDependencyVisitor(pdgGraph, cfgGraph);
        methodBody.accept(dataDependencyVisitor, null);
    }
}
