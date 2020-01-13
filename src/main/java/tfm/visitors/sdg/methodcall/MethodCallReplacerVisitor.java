package tfm.visitors.sdg.methodcall;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;
import tfm.utils.Context;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.variables.actions.VariableDeclaration;

import java.util.*;
import java.util.stream.Collectors;

public class MethodCallReplacerVisitor extends VoidVisitorAdapter<Context> {

    private PDGGraph pdgGraph;

    public MethodCallReplacerVisitor(PDGGraph pdgGraph) {
        this.pdgGraph = pdgGraph;
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Context context) {

        Optional<MethodDeclaration> optionalCallingMethod = methodCallExpr.getScope().isPresent()
                ? shouldMakeCallWithScope(methodCallExpr, context)
                : shouldMakeCallWithNoScope(methodCallExpr, context);

        if (!optionalCallingMethod.isPresent()) {
            return;
        }

        /* TODO

            1. Get parameters of the method declaration

            2. Parse arguments of the method call

            3. Build in and out assignment expressions

            4. Add in and out variable nodes

            5. Compute data dependency of new nodes

         */

        MethodDeclaration callingMethod = optionalCallingMethod.get();

        // 1
        List<String> parameterNames = callingMethod.getParameters().stream()
                .map(NodeWithSimpleName::getNameAsString)
                .collect(Collectors.toList());

        // todo: Add global variables

        // 2
        List<String> argumentNames = methodCallExpr.getArguments().stream()
                .map(Node::toString) // todo: argument can be a numeric, string, or method call expression. Must differentiate between them
                .collect(Collectors.toList());

        // todo: Add global variables

        // 3
        if (parameterNames.size() != argumentNames.size()) {
            // todo: The last parameter is varargs: wrap the last n arguments in an array
        }

        List<AssignExpr> assignExprs = Utils.emptyList();

        // IN assignments
        for (int i = 0; i < parameterNames.size(); i++) {
            AssignExpr assignExpr =
                    new AssignExpr(
                            new StringLiteralExpr(parameterNames.get(i) + "_in"),
                            new StringLiteralExpr(argumentNames.get(i)),
                            AssignExpr.Operator.ASSIGN
                    );

            assignExprs.add(assignExpr);
        }

        // OUT assignments
        // todo

        // 4
        int lastId = pdgGraph.getNodes().stream()
                .map(GraphNode::getId)
                .max(Integer::compareTo)
                .orElse(-1);

        for (AssignExpr assignExpr : assignExprs) {
            pdgGraph.addNode(new GraphNode<Node>(lastId + 1, assignExpr.toString(), assignExpr));
        }

        // 5
        // todo
        // For IN, get last defined variable
        // For OUT, get first used variable

        Logger.log(String.format("Method '%s' called", optionalCallingMethod.get().getNameAsString()));
    }

    private Optional<MethodDeclaration> shouldMakeCallWithScope(MethodCallExpr methodCallExpr, Context context) {
        assert methodCallExpr.getScope().isPresent();
        assert context.getCurrentClass().isPresent();

        String scopeName = methodCallExpr.getScope().get().toString();

        ClassOrInterfaceDeclaration currentClass = context.getCurrentClass().get();

        // Check if it's a static method call of current class
        if (!Objects.equals(scopeName, currentClass.getNameAsString())) {

            // Check if 'scopeName' is a variable
            List<GraphNode<?>> declarations = pdgGraph.findDeclarationsOfVariable(scopeName);

            if (declarations.isEmpty()) {
                // It is a static method call of another class TODO: check other classes in the same file (compilation unit)
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
