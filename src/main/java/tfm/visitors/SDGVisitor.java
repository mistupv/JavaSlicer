package tfm.visitors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edg.graphlib.Graph;
import edg.graphlib.Vertex;
import edg.graphlib.Visitor;
import tfm.arcs.data.ArcData;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.nodes.PDGNode;
import tfm.nodes.SDGNode;

import java.util.*;
import java.util.stream.Collectors;

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

                    String currentClassName = currentClassNode.getAstNode().getNameAsString();

                    // Check if it's a static method call of current class
                    if (!Objects.equals(scopeName, currentClassName)) {

                        // Check if 'scopeName' is a variable
                        List<SDGNode<?>> declarations = sdgGraph.findDeclarationsOfVariable(scopeName);

                        if (declarations.isEmpty()) {
                            // It is a static method call of another class. We don't do anything
                            return;
                        } else {
                            /*
                                It's a variable since it has declarations. We now have to check if the class name
                                is the same as the current class (the object is an instance of our class)
                            */
                            SDGNode<?> declarationNode = declarations.get(declarations.size() - 1);

                            ExpressionStmt declarationExpr = (ExpressionStmt) declarationNode.getAstNode();
                            VariableDeclarationExpr variableDeclarationExpr = declarationExpr.getExpression().asVariableDeclarationExpr();

                            Optional<VariableDeclarator> optionalVariableDeclarator = variableDeclarationExpr.getVariables().stream()
                                    .filter(variableDeclarator -> Objects.equals(variableDeclarator.getNameAsString(), scopeName))
                                    .findFirst();

                            if (!optionalVariableDeclarator.isPresent()) {
                                // should not happen
                                return;
                            }

                            Type variableType = optionalVariableDeclarator.get().getType();

                            if (!variableType.isClassOrInterfaceType()) {
                                // Not class type
                                return;
                            }

                            if (!Objects.equals(variableType.asClassOrInterfaceType().getNameAsString(), currentClassName)) {
                                // object is not instance of our class
                                return;
                            }

                            // if we got here, the object is instance of our class, so we make the call
                        }
                    }

                    // It's a static method call to a method of the current class

                }
            }
        };

        pdgcfgVisitor.visit(methodDeclaration, pdgGraph.getRootNode());


        sdgGraph.addNode(methodDeclaration.getNameAsString(), methodDeclaration);

        pdgGraph.breadthFirstSearch(pdgGraph.getRootNode(), (Visitor<String, ArcData>) (g, v) -> {
            PDGNode<?> pdgNode = (PDGNode) v;


        });

        pdgGraph.getNodes().stream().skip(1).forEach(pdgNode -> {
            Statement statement = (Statement) pdgNode.getAstNode();

            if (statement.isExpressionStmt()) {
                Expression expression = statement.asExpressionStmt().getExpression();

                expression.findFirst(MethodCallExpr.class).ifPresent(methodCallExpr -> {

                });
            } else {

            }
        });





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
