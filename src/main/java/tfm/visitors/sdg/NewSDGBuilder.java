package tfm.visitors.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import javassist.expr.MethodCall;
import tfm.graphbuilding.Graphs;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.utils.Context;

public class NewSDGBuilder extends VoidVisitorAdapter<Context> {

    SDGGraph sdgGraph;

    public NewSDGBuilder(SDGGraph sdgGraph) {
        this.sdgGraph = sdgGraph;
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Context context) {
        if (!methodDeclaration.getBody().isPresent()) {
            return;
        }

        context.setCurrentMethod(methodDeclaration);

        // Build PDG and add to SDGGraph
        PDGGraph pdgGraph = Graphs.PDG.fromASTNode(methodDeclaration);

        sdgGraph.addMethod(methodDeclaration, pdgGraph);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Context context) {
//        if (sdgGraph.getRootNode() != null) {
//            throw new IllegalStateException("¡Solo podemos procesar una clase por el momento!");
//        }

        if (classOrInterfaceDeclaration.isInterface()) {
            throw new IllegalArgumentException("¡Las interfaces no estan permitidas!");
        }

        context.setCurrentClass(classOrInterfaceDeclaration);

        classOrInterfaceDeclaration.accept(this, context);

        // Once every PDG is built, expand method declaration nodes of each one
        // todo methodDeclaration replacer


        // Once every PDG is built, expand method call nodes of each one
        // and link them to the corresponding method declaration node
        MethodCallReplacer methodCallReplacer = new MethodCallReplacer(sdgGraph);
        methodCallReplacer.replace();



        // 3. Build summary arcs
    }

    @Override
    public void visit(CompilationUnit compilationUnit, Context context) {
        context.setCurrentCU(compilationUnit);

        super.visit(compilationUnit, context);
    }
}
