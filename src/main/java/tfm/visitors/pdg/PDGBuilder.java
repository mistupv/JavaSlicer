package tfm.visitors.pdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;
import tfm.visitors.cfg.CFGBuilder;

public class PDGBuilder extends VoidVisitorAdapter<GraphNode<?>> {

    private PDGGraph pdgGraph;
    private CFGGraph cfgGraph;

    public PDGBuilder(PDGGraph pdgGraph) {
        this(pdgGraph, new CFGGraph() {
            @Override
            protected String getRootNodeData() {
                return "Start";
            }
        });
    }

    public PDGBuilder(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;

        this.pdgGraph.setCfgGraph(cfgGraph);
    }

    public void visit(MethodDeclaration methodDeclaration, GraphNode<?> parent) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        // Assign the method declaration to the root node of the PDG graph
        this.pdgGraph.getRootNode().setAstNode(methodDeclaration);

        BlockStmt methodBody = methodDeclaration.getBody().get();

        // build CFG
        methodBody.accept(new CFGBuilder(cfgGraph), null);

        // Build control dependency
        ControlDependencyBuilder controlDependencyBuilder = new ControlDependencyBuilder(pdgGraph, cfgGraph);
        methodBody.accept(controlDependencyBuilder, parent);

        // Build data dependency
        DataDependencyBuilder dataDependencyBuilder = new DataDependencyBuilder(pdgGraph, cfgGraph);
        methodBody.accept(dataDependencyBuilder, null);
    }
}
