package tfm.visitors;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGNode;
import tfm.variables.VariableSet;
import tfm.variables.VariableExtractor;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableDefinition;

public class PDGVisitor extends VoidVisitorAdapter<PDGNode> {

    private VariableSet variableSet;
    private PDGGraph graph;

    public PDGVisitor(PDGGraph graph) {
        this.graph = graph;
        this.variableSet = new VariableSet();
    }

    @Override
    public void visit(ExpressionStmt n, PDGNode parent) {
        Expression expression = n.getExpression();

        PDGNode expressionNode = graph.addNode(expression.toString(), n);

        graph.addControlDependencyArc(parent, expressionNode);

        new VariableExtractor()
                .setOnVariableDeclarationListener(variable ->
                        variableSet.addVariable(variable, new VariableDeclaration(expressionNode))
                ).setOnVariableDefinitionListener(variable ->
                        variableSet.addDefinition(variable, new VariableDefinition(expressionNode))
                ).setOnVariableUseListener(variable -> {
                    variableSet.getLastDefinitionOf(variable, expressionNode)
                        .ifPresent(variableDefinition -> graph.addDataDependencyArc(
                                (PDGNode) variableDefinition.getNode(),
                                expressionNode,
                                variable
                        ));
//                    variableSet.addUse(variable, new VariableUse(expressionNode));
                })
                .visit(expression);
    }

    @Override
    public void visit(IfStmt ifStmt, PDGNode parent) {
        PDGNode ifNode = graph.addNode(
                String.format("if (%s)", ifStmt.getCondition().toString()),
                ifStmt
        );

        graph.addControlDependencyArc(parent, ifNode);

        new VariableExtractor()
                .setOnVariableUseListener(variable -> {
                    variableSet.getLastDefinitionOf(variable, ifNode)
                            .ifPresent(variableDefinition -> graph.addDataDependencyArc(
                                    (PDGNode) variableDefinition.getNode(),
                                    ifNode,
                                    variable
                            ));
//                    variableSet.addUse(variable, new VariableUse(expressionNode));
                })
                .visit(ifStmt.getCondition());

        // Default adapter visits else before then, we have to visit then branch first
        ifStmt.getThenStmt().accept(this, ifNode);
        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, ifNode));
    }

    @Override
    public void visit(WhileStmt whileStmt, PDGNode parent) {
        // assert whileStmt.getBegin().isPresent();

        PDGNode whileNode = graph.addNode(
                String.format("while (%s)", whileStmt.getCondition().toString()),
                whileStmt
        );

        graph.addControlDependencyArc(parent, whileNode);

        new VariableExtractor()
                .setOnVariableUseListener(variable -> {
                    variableSet.getLastDefinitionOf(variable, whileNode)
                            .ifPresent(variableDefinition -> graph.addDataDependencyArc(
                                    (PDGNode) variableDefinition.getNode(),
                                    whileNode,
                                    variable
                            ));
                })
                .visit(whileStmt.getCondition());

        whileStmt.getBody().accept(this, whileNode);

    }

//    @Override
//    public void visit(ForStmt forStmt, PDGNode parent) {
//        // Add initialization nodes
//        forStmt.getInitialization().stream()
//                .map(expression -> graph.addNode(expression.toString()))
//                .forEach(pdgVertex -> graph.addControlDependencyArc(parent, pdgVertex));
//
//        // Add condition node
//        Expression condition = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));
//        PDGNode conditionNode = graph.addNode(condition.toString());
//
//        graph.addControlDependencyArc(parent, conditionNode);
//
//        // Visit for
//        super.visit(forStmt, conditionNode);
//
//        // Add update vertex
//        forStmt.getUpdate().stream()
//                .map(expression -> graph.addNode(expression.toString()))
//                .forEach(pdgVertex -> graph.addControlDependencyArc(conditionNode, pdgVertex));
//    }

    @Override
    public void visit(ForEachStmt forEachStmt, PDGNode parent) {
//        // Initializer
//        VariableDeclarationExpr iterator = new VariableDeclarationExpr(
//                new VariableDeclarator(
//                        JavaParser.parseClassOrInterfaceType("Iterator"),
//                        "iterator",
//                        new ConditionalExpr(
//                                new MethodCallExpr(
//                                        new MethodCallExpr(
//                                            forEachStmt.getIterable(),
//                                            "getClass"
//                                        ),
//                                        "isArray"
//                                ),
//                                new MethodCallExpr(
//                                        new NameExpr("Arrays"),
//                                        "asList",
//                                        new NodeList<>(
//                                                forEachStmt.getIterable()
//                                        )
//                                ),
//                                new CastExpr(
//                                        JavaParser.parseClassOrInterfaceType("Iterable"),
//                                        new CastExpr(
//                                                JavaParser.parseClassOrInterfaceType("Object"),
//                                                forEachStmt.getIterable()
//                                        )
//                                )
//                        )
//                )
//        );
//
//        // Compare
//        MethodCallExpr iteratorHasNext = new MethodCallExpr(
//                new NameExpr("iterator"),
//                "hasNext"
//        );
//
//        // Body
//        Type variableType = forEachStmt.getVariable().getCommonType();
//        String variableName = forEachStmt.getVariable().getVariables().get(0).getNameAsString();
//
//        BlockStmt foreachBody = Utils.blockWrapper(forEachStmt.getBody());
//        foreachBody.getStatements().addFirst(
//                new ExpressionStmt(
//                    new VariableDeclarationExpr(
//                            new VariableDeclarator(
//                                    variableType,
//                                    variableName,
//                                    new CastExpr(
//                                            variableType,
//                                            new MethodCallExpr(
//                                                    new NameExpr("iterator"),
//                                                    "next"
//                                            )
//                                    )
//                            )
//                    )
//                )
//        );
//
//        new ForStmt(new NodeList<>(iterator), iteratorHasNext, new NodeList<>(), foreachBody)
//            .accept(this, parent);


    }

//    @Override
//    public void visit(SwitchStmt switchStmt, PDGNode parent) {
//        PDGNode switchNode = graph.addNode(switchStmt.toString());
//
//        graph.addControlDependencyArc(parent, switchNode);
//
//        switchStmt.getSelector().accept(this, parent);
//        switchStmt.getEntries()
//                .forEach(switchEntryStmt -> switchEntryStmt.accept(this, switchNode));
//    }
}
