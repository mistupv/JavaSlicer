package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.pdg.PDG;

import java.util.ArrayList;
import java.util.List;

/**
 * 31/8/19
 * Asumimos que procesamos 1 archivo con una o más clases donde el primer método de la primera clase es el main
 *
 */
class SDGBuilder extends VoidVisitorAdapter<Void> {

    SDG sdg;
    List<PDG> pdgs;

    private ClassOrInterfaceDeclaration currentClass;
    private CompilationUnit currentCompilationUnit;

    protected SDGBuilder(SDG sdg) {
        this.sdg = sdg;
        this.pdgs = new ArrayList<>();
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void ignored) {
        if (!methodDeclaration.getBody().isPresent())
            return;


        if (sdg.isEmpty()) {
            sdg.addNode("ENTER " + methodDeclaration.getNameAsString(), methodDeclaration);
        } else {
//            sdgGraph.addMethod(methodDeclaration);
        }

        PDG pdg = new PDG();

        // TODO: this should happen in the PDG's creation, not here.
//        PDGBuilder PDGBuilder = new PDGBuilder(pdg) {
//            @Override
//            public void visit(MethodCallExpr methodCallExpr, Void empty) {
//                if (methodCallExpr.getScope().isPresent()) {
//                    String scopeName = methodCallExpr.getScope().get().toString();
//
//                    String currentClassName = currentClass.getNameAsString();
//
//                    // Check if it's a static method call of current class
//                    if (!Objects.equals(scopeName, currentClassName)) {
//
//                        // Check if 'scopeName' is a variable
//                        List<GraphNode<?>> declarations = sdg.findDeclarationsOfVariable(scopeName);
//
//                        if (declarations.isEmpty()) {
//                            // It is a static method call of another class. We don't do anything
//                            return;
//                        } else {
//                            /*
//                                It's a variable since it has declarations. We now have to check if the class name
//                                is the same as the current class (the object is an instance of our class)
//                            */
//                            GraphNode<?> declarationNode = declarations.get(declarations.size() - 1);
//
//                            ExpressionStmt declarationExpr = (ExpressionStmt) declarationNode.getAstNode();
//                            VariableDeclarationExpr variableDeclarationExpr = declarationExpr.getExpression().asVariableDeclarationExpr();
//
//                            Optional<VariableDeclarator> optionalVariableDeclarator = variableDeclarationExpr.getVariables().stream()
//                                    .filter(variableDeclarator -> Objects.equals(variableDeclarator.getNameAsString(), scopeName))
//                                    .findFirst();
//
//                            if (!optionalVariableDeclarator.isPresent()) {
//                                // should not happen
//                                return;
//                            }
//
//                            Type variableType = optionalVariableDeclarator.get().getType();
//
//                            if (!variableType.isClassOrInterfaceType()) {
//                                // Not class type
//                                return;
//                            }
//
//                            if (!Objects.equals(variableType.asClassOrInterfaceType().getNameAsString(), currentClassName)) {
//                                // object is not instance of our class
//                                return;
//                            }
//
//                            // if we got here, the object is instance of our class, so we make the call
//                        }
//                    }
//
//                    // It's a static method call to a method of the current class
//
//                }
//            }
//        };

//        PDGBuilder.createFrom(methodDeclaration);


        sdg.addNode(methodDeclaration.getNameAsString(), methodDeclaration);

        pdg.vertexSet().stream().skip(1).forEach(pdgNode -> {
            Statement statement = (Statement) pdgNode.getAstNode();

            if (statement.isExpressionStmt()) {
                Expression expression = statement.asExpressionStmt().getExpression();

                expression.findFirst(MethodCallExpr.class).ifPresent(methodCallExpr -> {

                });
            } else {

            }
        });





        sdg.addPDG(pdg, methodDeclaration);

        methodDeclaration.accept(this, ignored);

        pdgs.add(pdg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, Void ignored) {
//        if (sdgGraph.getRootNode() != null) {
//            throw new IllegalStateException("¡Solo podemos procesar una clase por el momento!");
//        }

        if (classOrInterfaceDeclaration.isInterface()) {
            throw new IllegalArgumentException("¡Las interfaces no estan permitidas!");
        }

        currentClass = classOrInterfaceDeclaration;

        classOrInterfaceDeclaration.accept(this, ignored);
    }

    @Override
    public void visit(CompilationUnit compilationUnit, Void ignored) {
        currentCompilationUnit = compilationUnit;

        super.visit(compilationUnit, ignored);
    }
}
