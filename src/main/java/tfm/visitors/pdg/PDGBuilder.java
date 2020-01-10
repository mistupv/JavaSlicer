package tfm.visitors.pdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;
import tfm.visitors.cfg.CFGBuilder;

/**
 * Populates a {@link PDGGraph}, given a complete {@link CFGGraph}, an empty {@link PDGGraph} and an AST root node.
 * For now it only accepts {@link MethodDeclaration} as root, as it can only receive a single CFG.
 * <br/>
 * <b>Usage:</b>
 * <ol>
 *     <li>Create an empty {@link CFGGraph}.</li>
 *     <li>Create an empty {@link PDGGraph} (optionally passing the {@link CFGGraph} as argument).</li>
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
        methodDeclaration.accept(new CFGBuilder(cfgGraph), null);

        // Build control dependency
        ControlDependencyBuilder controlDependencyBuilder = new ControlDependencyBuilder(pdgGraph, cfgGraph);
        controlDependencyBuilder.analyze();

        // Build data dependency
        DataDependencyBuilder dataDependencyBuilder = new DataDependencyBuilder(pdgGraph, cfgGraph);
        methodBody.accept(dataDependencyBuilder, null);
    }
}
