package tfm.graphs.sdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.nodes.GraphNode;
import tfm.nodes.factories.InVariableNodeFactory;
import tfm.nodes.factories.OutVariableNodeFactory;
import tfm.utils.Context;
import tfm.utils.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class MethodCallReplacerVisitor extends VoidVisitorAdapter<Context> {

    private SDG sdg;
    private GraphNode<?> methodCallNode;

    public MethodCallReplacerVisitor(SDG sdg) {
        this.sdg = sdg;
    }

    private void searchAndSetMethodCallNode(Node node) {
        Optional<GraphNode<Node>> optionalNode = sdg.findNodeByASTNode(node);
        assert optionalNode.isPresent();
        methodCallNode = optionalNode.get();

    }

    @Override
    public void visit(DoStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ForEachStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ForStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(IfStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(SwitchStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(WhileStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ReturnStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ExpressionStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Context context) {
        List<Expression> arguments = methodCallExpr.getArguments();

//        // Parse first method call expressions as arguments
//        arguments.stream()
//                .filter(Expression::isMethodCallExpr)
//                .forEach(expression -> expression.accept(this, context));

        Logger.log("MethodCallReplacerVisitor", context);

        Optional<MethodDeclaration> optionalCallingMethod = methodCallExpr.getScope().isPresent()
                ? shouldMakeCallWithScope(methodCallExpr, context)
                : shouldMakeCallWithNoScope(methodCallExpr, context);

        if (!optionalCallingMethod.isPresent()) {
            Logger.log("Discarding: " + methodCallExpr);
            return;
        }

        MethodDeclaration methodCalled = optionalCallingMethod.get();

        Optional<GraphNode<MethodDeclaration>> optionalCalledMethodNode = sdg.findNodeByASTNode(methodCalled);

        assert optionalCalledMethodNode.isPresent();

        GraphNode<MethodDeclaration> calledMethodNode = optionalCalledMethodNode.get();

        sdg.addCallArc(methodCallNode, calledMethodNode);

        NodeList<Parameter> parameters = calledMethodNode.getAstNode().getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            Expression argument = arguments.get(i);

            // In expression
            VariableDeclarationExpr inVariableDeclarationExpr = new VariableDeclarationExpr(
                    new VariableDeclarator(
                            parameter.getType(),
                            parameter.getNameAsString() + "_in",
                            new NameExpr(argument.toString())
                    )
            );

            ExpressionStmt inExprStmt = new ExpressionStmt(inVariableDeclarationExpr);

            GraphNode<ExpressionStmt> argumentInNode = sdg.addNode(inExprStmt.toString(), inExprStmt, new InVariableNodeFactory());

            // Out expression
            VariableDeclarationExpr outVariableDeclarationExpr = new VariableDeclarationExpr(
                    new VariableDeclarator(
                            parameter.getType(),
                            parameter.getNameAsString(),
                            new NameExpr(parameter.getNameAsString() + "_out")
                    )
            );

            ExpressionStmt outExprStmt = new ExpressionStmt(outVariableDeclarationExpr);

            GraphNode<ExpressionStmt> argumentOutNode = sdg.addNode(outExprStmt.toString(), outExprStmt, new OutVariableNodeFactory());


        }

        // todo make call
        Logger.log("MethodCallReplacerVisitor", String.format("%s | Method '%s' called", methodCallExpr, methodCalled.getNameAsString()));
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
            List<GraphNode<?>> declarations = sdg.findDeclarationsOfVariable(scopeName, methodCallNode);

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
        int argumentsCount = methodCallExpr.getArguments().size();

        // Get methods with equal name and parameter count
        List<MethodDeclaration> classMethods = klass.getMethodsByName(methodCallExpr.getNameAsString()).stream()
                // Filter methods with equal or less (varargs) number of parameters
                .filter(methodDeclaration -> {
                    NodeList<Parameter> parameters = methodDeclaration.getParameters();

                    if (parameters.size() == argumentsCount) {
                        return true;
                    }

                    if (parameters.isEmpty()) {
                        return false;
                    }

                    // There are more arguments than parameters. May be OK if last parameter is varargs
                    if (parameters.size() < argumentsCount) {
                        return parameters.get(parameters.size() - 1).isVarArgs();
                    }

                    // There are less arguments than parameters. May be OK if the last parameter is varargs
                    // and it's omited
                    return parameters.size() - 1 == argumentsCount
                            && parameters.get(parameters.size() - 1).isVarArgs();
                })
                .collect(Collectors.toList());

        if (classMethods.isEmpty()) {
            // No methods in class with that name and parameter count
            return Optional.empty();
        }

        if (classMethods.size() == 1) {
            // We found the method!
            return Optional.of(classMethods.get(0));
        }

        /*
         * Tricky one! We have to match argument and parameter types, so we have to:
         *   - Differentiate arguments expressions:
         *       - Easy: In case of CastExpr, get the type
         *       - Easy: In case of ObjectCreationExpr, get the type
         *       - Medium: In case of NameExpr, find the declaration and its type
         *       - Medium: In case of LiteralExpr, get the type
         *       - Hard: In case of MethodCallExpr, find MethodDeclaration and its type
         *   - If there is a varargs parameter, check every argument corresponding to it has the same type
         *
         * Example:
         *   At this point these three methods are considered as called:
         *    private void foo(int a, int b) {}
         *    private void foo(String a, String b) {}
         *    private void foo(String a, int... bs) {}
         *
         * We have to match types to get the correct one
         *
         * */

        return classMethods.stream().filter(methodDeclaration -> {
            boolean match = true;

            for (int i = 0; i < methodDeclaration.getParameters().size(); i++) {
                if (!match) {
                    break;
                }

                if (argumentsCount < i) {
                    return argumentsCount == i - 1
                            && methodDeclaration.getParameter(i).isVarArgs();
                }

                // TODO - Convert into a visitor

                Expression argumentExpression = methodCallExpr.getArgument(i);
                Parameter parameter = methodDeclaration.getParameter(i);

                if (argumentExpression.isCastExpr()) {
                    match = Objects.equals(argumentExpression.asCastExpr().getType(), parameter.getType());
                } else if (argumentExpression.isObjectCreationExpr()) {
                    match = Objects.equals(argumentExpression.asObjectCreationExpr().getType(), parameter.getType());
                } else if (argumentExpression.isNameExpr()) {
                    String variableName = argumentExpression.asNameExpr().getNameAsString();

                    List<GraphNode<?>> declarationsOfVariable = sdg.findDeclarationsOfVariable(argumentExpression.asNameExpr().getNameAsString(), methodCallNode);

                    assert !declarationsOfVariable.isEmpty();

                    GraphNode<?> declarationNode = declarationsOfVariable.get(declarationsOfVariable.size() - 1);

                    ExpressionStmt expressionStmt = (ExpressionStmt) declarationNode.getAstNode();

                    assert expressionStmt.getExpression().isVariableDeclarationExpr();

                    match = expressionStmt.getExpression().asVariableDeclarationExpr().getVariables().stream()
                            .filter(variableDeclarator -> Objects.equals(variableDeclarator.getName().asString(), variableName))
                            .findFirst()
                            .map(variableDeclarator -> Objects.equals(variableDeclarator.getType(), parameter.getType()))
                            .orElse(false);
                } // TODO: More checks
            }

            return match;
        }).findFirst();
    }
}
