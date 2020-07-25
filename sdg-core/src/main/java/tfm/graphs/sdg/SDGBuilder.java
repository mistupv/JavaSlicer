package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.pdg.PDG;
import tfm.utils.Context;

public class SDGBuilder extends VoidVisitorAdapter<Context> {

    SDG sdg;

    public SDGBuilder(SDG sdg) {
        this.sdg = sdg;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Context context) {
        if (methodDeclaration.getBody().isEmpty())
            return;
        context.setCurrentMethod(methodDeclaration);
        buildAndCopyPDG(methodDeclaration);
    }

    protected PDG createPDG() {
        return new PDG();
    }

    protected void buildAndCopyPDG(MethodDeclaration methodDeclaration) {
        PDG pdg = createPDG();
        pdg.build(methodDeclaration);
        pdg.vertexSet().forEach(sdg::addNode);
        pdg.edgeSet().forEach(arc -> sdg.addEdge(pdg.getEdgeSource(arc), pdg.getEdgeTarget(arc), arc));
        sdg.setMethodCFG(pdg.getCfg());
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Context context) {
        if (classOrInterfaceDeclaration.isInterface())
            throw new IllegalArgumentException("¡Las interfaces no estan permitidas!");
        context.setCurrentClass(classOrInterfaceDeclaration);
        super.visit(classOrInterfaceDeclaration, context);
    }

    @Override
    public void visit(CompilationUnit compilationUnit, Context context) {
        context.setCurrentCU(compilationUnit);
        super.visit(compilationUnit, context);
    }
}
