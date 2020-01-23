package tfm.visitors.pdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFG;
import tfm.graphs.PDG;
import tfm.visitors.cfg.CFGBuilder;

public class PDGBuilder extends VoidVisitorAdapter<Void> {

    private PDG pdg;
    private CFG cfg;

    public PDGBuilder(PDG pdg) {
        this(pdg, new CFG());
    }

    public PDGBuilder(PDG pdg, CFG cfg) {
        this.pdg = pdg;
        this.cfg = cfg;

        this.pdg.setCfg(cfg);
    }

    public void visit(MethodDeclaration methodDeclaration, Void empty) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        this.pdg.buildRootNode("ENTER " + methodDeclaration.getNameAsString(), methodDeclaration);

        assert this.pdg.getRootNode().isPresent();

        // build CFG
        methodDeclaration.accept(new CFGBuilder(cfg), null);

        BlockStmt methodBody = methodDeclaration.getBody().get();

        // Build control dependency
        ControlDependencyBuilder controlDependencyBuilder = new ControlDependencyBuilder(pdg, cfg);
        methodBody.accept(controlDependencyBuilder, pdg.getRootNode().get());

        // Build data dependency
        DataDependencyBuilder dataDependencyBuilder = new DataDependencyBuilder(pdg, cfg);
        methodBody.accept(dataDependencyBuilder, null);
    }
}
