package tfm.graphs.sdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.pdg.PDG;
import tfm.nodes.GraphNode;
import tfm.utils.Context;
import tfm.utils.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class MethodCallReplacerVisitor extends VoidVisitorAdapter<Context> {

    private PDG pdg;

    public MethodCallReplacerVisitor(PDG pdg) {
        this.pdg = pdg;
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Context context) {

        Optional<MethodDeclaration> optionalCallingMethod = methodCallExpr.getScope().isPresent()
                ? shouldMakeCallWithScope(methodCallExpr, context)
                : shouldMakeCallWithNoScope(methodCallExpr, context);

        if (!optionalCallingMethod.isPresent()) {
            return;
        }

        // todo make call
        Logger.log(String.format("Method '%s' called", optionalCallingMethod.get().getNameAsString()));
    }

    private Optional<MethodDeclaration> shouldMakeCallWithScope(MethodCallExpr methodCallExpr, Context context) {
        assert methodCallExpr.getScope().isPresent();

        String scopeName = methodCallExpr.getScope().get().toString();

        if (!context.getCurrentClass().isPresent()) {
            return Optional.empty();
        }

        ClassOrInterfaceDeclaration currentClass = context.getCurrentClass().get();

        // Check if it's a static method call of current class
        if (!Objects.equals(scopeName, currentClass.getNameAsString())) {

            // Check if 'scopeName' is a variable
            List<GraphNode<?>> declarations = pdg.findDeclarationsOfVariable(scopeName);

            if (declarations.isEmpty()) {
                // It is a static method call of another class. We do nothing
                return Optional.empty();
            }

            /*
                It's a variable since it has declarations. We now have to check if the class name
                is the same as the current class (the object is an instance of our class)
            */
            GraphNode<?> declarationNode = declarations.get(declarations.size() - 1);

            ExpressionStmt declarationExpr = (ExpressionStmt) declarationNode.getAstNode();
            VariableDeclarationExpr variableDeclarationExpr = declarationExpr.getExpression().asVariableDeclarationExpr();

            Optional<VariableDeclarator> optionalVariableDeclarator = variableDeclarationExpr.getVariables().stream()
                    .filter(variableDeclarator -> Objects.equals(variableDeclarator.getNameAsString(), scopeName))
                    .findFirst();

            if (!optionalVariableDeclarator.isPresent()) {
                // should not happen
                return Optional.empty();
            }

            Type variableType = optionalVariableDeclarator.get().getType();

            if (!variableType.isClassOrInterfaceType()) {
                // Not class type
                return Optional.empty();
            }

            if (!Objects.equals(variableType.asClassOrInterfaceType().getNameAsString(), currentClass.getNameAsString())) {
                // object is not instance of our class
                return Optional.empty();
            }

            // if we got here, the object is instance of our class
        }

        // It's a static method call to a method of the current class

        return findMethodInClass(methodCallExpr, currentClass);
    }

    private Optional<MethodDeclaration> shouldMakeCallWithNoScope(MethodCallExpr methodCallExpr, Context context) {
        assert !methodCallExpr.getScope().isPresent();

        /*
            May be a call to a method of the current class or a call to an imported static method.
            In the first case, we make the call. Otherwise, not.
        */

        if (!context.getCurrentClass().isPresent()) {
            return Optional.empty();
        }

        // We get the current class and search along their methods to find the one we're looking for...

        ClassOrInterfaceDeclaration currentClass = context.getCurrentClass().get();

        return findMethodInClass(methodCallExpr, currentClass);
    }

    private Optional<MethodDeclaration> findMethodInClass(MethodCallExpr methodCallExpr, ClassOrInterfaceDeclaration klass) {
        String[] typeParameters = methodCallExpr.getTypeArguments()
                .map(types -> types.stream()
                        .map(Node::toString)
                        .collect(Collectors.toList())
                        .toArray(new String[types.size()])
                ).orElse(new String[]{});

        List<MethodDeclaration> classMethods =
                klass.getMethodsBySignature(methodCallExpr.getNameAsString(), typeParameters);

        if (classMethods.isEmpty()) {
            return Optional.empty(); // The method called is not inside the current class
        }

        // The current method is inside the current class, so we make the call
        return Optional.of(classMethods.get(0));
    }
}
