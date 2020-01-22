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

        this.pdgGraph.buildRootNode("ENTER " + methodDeclaration.getNameAsString(), methodDeclaration);

        assert this.pdgGraph.getRootNode().isPresent();

        // build CFG
        methodDeclaration.accept(new CFGBuilder(cfgGraph), null);

        BlockStmt methodBody = methodDeclaration.getBody().get();

        // Build control dependency
        ControlDependencyBuilder controlDependencyBuilder = new ControlDependencyBuilder(pdgGraph, cfgGraph);
        methodBody.accept(controlDependencyBuilder, pdgGraph.getRootNode().get());

        // Build data dependency
        DataDependencyBuilder dataDependencyBuilder = new DataDependencyBuilder(pdgGraph, cfgGraph);
        methodBody.accept(dataDependencyBuilder, null);
    }
}
