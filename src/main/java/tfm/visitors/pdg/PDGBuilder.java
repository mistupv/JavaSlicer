package tfm.visitors.pdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.visitors.cfg.CFGBuilder;

public class PDGBuilder extends VoidVisitorAdapter<Void> {

    private PDGGraph pdgGraph;
    private CFGGraph cfgGraph;

    public PDGBuilder(PDGGraph pdgGraph) {
        this(pdgGraph, new CFGGraph());
    }

    public PDGBuilder(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;

        this.pdgGraph.setCfgGraph(cfgGraph);
    }

    public void visit(MethodDeclaration methodDeclaration, Void empty) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        // Assign the method declaration to the root node of the PDG graph. Here parent will always be the root node
        this.pdgGraph.modifyNode(pdgGraph.getRootNode().getId(), mutableGraphNode -> {
            mutableGraphNode.setInstruction("ENTER " + methodDeclaration.getNameAsString());
            mutableGraphNode.setAstNode(methodDeclaration);
        });

        BlockStmt methodBody = methodDeclaration.getBody().get();

        // build CFG
        methodBody.accept(new CFGBuilder(cfgGraph), null);

        // Build control dependency
        ControlDependencyBuilder controlDependencyBuilder = new ControlDependencyBuilder(pdgGraph, cfgGraph);
        methodBody.accept(controlDependencyBuilder, pdgGraph.getRootNode());

        // Build data dependency
        DataDependencyBuilder dataDependencyBuilder = new DataDependencyBuilder(pdgGraph, cfgGraph);
        methodBody.accept(dataDependencyBuilder, null);
    }
}
