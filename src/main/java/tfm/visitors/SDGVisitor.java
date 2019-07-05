package tfm.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;

import java.util.ArrayList;
import java.util.List;

public class SDGVisitor extends VoidVisitorAdapter<Void> {

    SDGGraph sdgGraph;

    List<PDGGraph> allPDGs = new ArrayList<>();

    public SDGVisitor(SDGGraph sdgGraph) {
        this.sdgGraph = sdgGraph;
    }

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
