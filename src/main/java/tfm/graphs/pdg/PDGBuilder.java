package tfm.graphs.pdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;

/**
 * Populates a {@link PDG}, given a complete {@link CFG}, an empty {@link PDG} and an AST root node.
 * For now it only accepts {@link MethodDeclaration} as root, as it can only receive a single CFG.
 * <br/>
 * <b>Usage:</b>
 * <ol>
 *     <li>Create an empty {@link CFG}.</li>
 *     <li>Create an empty {@link PDG} (optionally passing the {@link CFG} as argument).</li>
 *     <li>Create a new {@link PDGBuilder}, passing both graphs as arguments.</li>
 *     <li>Accept the builder as a visitor of the {@link MethodDeclaration} you want to analyse using
 *     {@link com.github.javaparser.ast.Node#accept(com.github.javaparser.ast.visitor.VoidVisitor, Object) Node#accept(VoidVisitor, Object)}:
 *     {@code methodDecl.accept(builder, null)}</li>
 *     <li>Once the previous step is finished, the complete PDG is saved in
 *     the object created in the second step. The builder should be discarded
 *     and not reused.</li>
 * </ol>
 */
public class PDGBuilder {
    private PDG pdg;
    private CFG cfg;

    protected PDGBuilder(PDG pdg) {
        assert pdg.getCfg() != null;
        this.pdg = pdg;
        this.cfg = pdg.getCfg();
    }

    public void createFrom(MethodDeclaration methodDeclaration) {
        if (!methodDeclaration.getBody().isPresent())
            throw new IllegalStateException("Method needs to have a body");

        this.pdg.buildRootNode("ENTER " + methodDeclaration.getNameAsString(), methodDeclaration);

        assert this.pdg.getRootNode().isPresent();

        BlockStmt methodBody = methodDeclaration.getBody().get();

        // build CFG
        if (!cfg.isBuilt())
            cfg.build(methodDeclaration);

        // Copy nodes from CFG to PDG
        for (GraphNode<?> node : cfg.vertexSet())
            if (!node.equals(cfg.getExitNode()))
                pdg.addVertex(node);

        // Build control dependency
        ControlDependencyBuilder controlDependencyBuilder = new ControlDependencyBuilder(pdg, cfg);
        controlDependencyBuilder.analyze();

        // Build data dependency
        DataDependencyBuilder dataDependencyBuilder = new DataDependencyBuilder(pdg, cfg);
        methodBody.accept(dataDependencyBuilder, null);
    }
}
