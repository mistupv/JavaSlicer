package tfm.visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edg.graphlib.Arrow;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.*;

public class PDGCFGVisitor extends VoidVisitorAdapter<PDGNode> {

    private PDGGraph pdgGraph;
    private CFGGraph cfgGraph;

    public CFGGraph getCfgGraph() {
        return cfgGraph;
    }

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

    public void visit(MethodDeclaration methodDeclaration, PDGNode parent) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        BlockStmt blockStmt = methodDeclaration.getBody().get();

        // Build control dependency
        ControlDependencyVisitor controlDependencyVisitor = new ControlDependencyVisitor(pdgGraph, cfgGraph);
        blockStmt.accept(controlDependencyVisitor, parent);

        // Build data dependency
        DataDependencyVisitor dataDependencyVisitor = new DataDependencyVisitor(pdgGraph, cfgGraph);
        blockStmt.accept(dataDependencyVisitor, null);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, PDGNode parent) {
        // build CFG
        classOrInterfaceDeclaration.accept(new CFGVisitor(cfgGraph), null);

        // Visit normally...
        super.visit(classOrInterfaceDeclaration, parent);
    }
}
