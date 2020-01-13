package tfm.visitors.pdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFG;
import tfm.graphs.PDG;
import tfm.nodes.GraphNode;
import tfm.visitors.cfg.CFGBuilder;

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
public class PDGBuilder extends VoidVisitorAdapter<GraphNode<?>> {

    private PDG pdg;
    private CFG cfg;

    public PDGBuilder(PDG pdg) {
        this(pdg, new CFG() {
            @Override
            protected String getRootNodeData() {
                return "Start";
            }
        });
    }

    public PDGBuilder(PDG pdg, CFG cfg) {
        this.pdg = pdg;
        this.cfg = cfg;

        this.pdg.setCfg(cfg);
    }

    public void visit(MethodDeclaration methodDeclaration, GraphNode<?> parent) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        // Assign the method declaration to the root node of the PDG graph
        this.pdg.getRootNode().setAstNode(methodDeclaration);

        BlockStmt methodBody = methodDeclaration.getBody().get();

        // build CFG
        methodDeclaration.accept(new CFGBuilder(cfg), null);

        // Build control dependency
        ControlDependencyBuilder controlDependencyBuilder = new ControlDependencyBuilder(pdg, cfg);
        controlDependencyBuilder.analyze();

        // Build data dependency
        DataDependencyBuilder dataDependencyBuilder = new DataDependencyBuilder(pdg, cfg);
        methodBody.accept(dataDependencyBuilder, null);
    }
}
