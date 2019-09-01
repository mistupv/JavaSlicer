package tfm.visitors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.PDGNode;
import tfm.nodes.SDGNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 31/8/19
 * Asumimos que procesamos 1 clase con uno o mas metodos estaticos donde el primer metodo es el main
 *
 */
public class SDGVisitor extends VoidVisitorAdapter<Void> {

    SDGGraph sdgGraph;
    List<PDGGraph> pdgGraphs;
    private SDGNode<ClassOrInterfaceDeclaration> currentClassNode;

    public SDGVisitor(SDGGraph sdgGraph) {
        this.sdgGraph = sdgGraph;
        this.pdgGraphs = new ArrayList<>();
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void ignored) {
        if (sdgGraph.getRootNode() != null) {
            throw new IllegalStateException("¡Solo podemos procesar una clase por el momento!");
        }

        if (classOrInterfaceDeclaration.isInterface()) {
            throw new IllegalArgumentException("¡Las interfaces no estan permitidas!");
        }

        currentClassNode = sdgGraph.addNode(
                "class " + classOrInterfaceDeclaration.getNameAsString(),
                classOrInterfaceDeclaration
        );

        sdgGraph.setRootVertex(currentClassNode);

        classOrInterfaceDeclaration.accept(this, ignored);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void ignored) {
        if (!methodDeclaration.getBody().isPresent())
            return;

        PDGGraph pdgGraph = new PDGGraph();

        PDGCFGVisitor pdgcfgVisitor = new PDGCFGVisitor(pdgGraph) {
            @Override
            public void visit(MethodCallExpr methodCallExpr, PDGNode<?> parent) {
                if (methodCallExpr.getScope().isPresent()) {
                    String scopeName = methodCallExpr.getScope().get().toString();

                    if (Objects.equals(scopeName, currentClassNode.getAstNode())) {

                    }
                }
            }
        };

        pdgcfgVisitor.visit(methodDeclaration, pdgGraph.getRootNode());

        sdgGraph.addPDG(pdgGraph, methodDeclaration);

        methodDeclaration.accept(this, ignored);

        pdgGraphs.add(pdgGraph);
    }

//    @Override
//    public void visit(CompilationUnit compilationUnit, Void ignored) {
//        super.visit(compilationUnit, ignored);
//
//
//    }
}
