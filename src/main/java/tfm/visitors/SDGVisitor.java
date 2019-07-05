package tfm.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;

import java.util.ArrayList;
import java.util.List;

public class SDGVisitor extends VoidVisitorAdapter<Void> {

    List<PDGGraph> allPDGs = new ArrayList<>();

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void ignored) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        PDGGraph pdgGraph = new PDGGraph();

        PDGCFGVisitor pdgcfgVisitor = new PDGCFGVisitor(pdgGraph);

        pdgcfgVisitor.visit(methodDeclaration.getBody().get(), pdgGraph.getRootNode());

        allPDGs.add(pdgGraph);
    }
}
