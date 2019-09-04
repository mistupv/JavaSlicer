package tfm.visitors;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.nodes.GraphNode;
import tfm.scopes.ScopeHolder;

@Deprecated
public class PDGVisitor extends VoidVisitorAdapter<ScopeHolder<GraphNode<?>>> {

//    private VariableSet variableSet;

//    protected PDGGraph graph;
//    protected ScopeHolder<PDGNode> globalScope;
//
//    public PDGVisitor(PDGGraph graph, ScopeHolder<PDGNode> scopeHolder) {
//        this.graph = graph;
//        this.globalScope = scopeHolder;
//    }
//
//    @Override
//    public void visit(ExpressionStmt n, ScopeHolder<PDGNode> scope) {
//        Expression expression = n.getExpression();
//
//        PDGNode expressionNode = graph.addNode(expression.toString(), n);
//
//        graph.addControlDependencyArc(scope.getRoot(), expressionNode);
//
//        VariableScope<PDGNode> expressionScope = new VariableScope<>(expressionNode);
//
//        new VariableExtractor()
//                .setOnVariableDeclarationListener(variable ->
//                        expressionScope.addVariableDeclaration(variable, expressionNode)
//                ).setOnVariableDefinitionListener(variable ->
//                        expressionScope.addVariableDefinition(variable, expressionNode)
//                ).setOnVariableUseListener(variable -> {
//                        expressionScope.addVariableUse(variable, expressionNode);
//
//                        Scope<PDGNode> searchScope = scope.isVariableDefined(variable) ? scope : globalScope;
//
//                        searchScope.getLastDefinitions(variable)
//                            .forEach(variableDefinition -> graph.addDataDependencyArc(
//                                    variableDefinition.getNode(),
//                                    expressionNode,
//                                    variable
//                            ));
//                })
//                .visit(expression);
//
//        scope.addSubscope(expressionScope);
//    }
//
//    @Override
//    public void visit(IfStmt ifStmt, ScopeHolder<PDGNode> scope) {
//        PDGNode ifNode = graph.addNode(
//                String.format("if (%s)", ifStmt.getCondition().toString()),
//                ifStmt
//        );
//
//        graph.addControlDependencyArc(scope.getRoot(), ifNode);
//
//        ScopeHolder<PDGNode> ifScope = ifStmt.hasElseBranch() ? new IfElseScope<>(ifNode) : new ScopeHolder<>(ifNode);
//
//        new VariableExtractor()
//                .setOnVariableUseListener(variable -> {
//                    ifScope.addVariableUse(variable, ifNode);
//
//                    Scope<PDGNode> searchScope = scope.isVariableDefined(variable) ? scope : globalScope;
//
//                    searchScope.getLastDefinitions(variable)
//                            .forEach(variableDefinition ->
//                                    graph.addDataDependencyArc(
//                                        variableDefinition.getNode(),
//                                        ifNode,
//                                        variable
//                                    )
//                            );
//                })
//                .visit(ifStmt.getCondition());
//
//        if (!ifStmt.hasElseBranch()) {
//            ifStmt.getThenStmt().accept(this, ifScope);
//        } else {
//            @SuppressWarnings("unchecked")
//            IfElseScope<PDGNode> ifElseScope = (IfElseScope<PDGNode>) ifScope;
//
//            ifStmt.getThenStmt().accept(this, ifElseScope.getThenScope());
//            ifStmt.getElseStmt().get().accept(this, ifElseScope.getElseScope());
//        }
//
//        scope.addSubscope(ifScope);
//    }
//
//    @Override
//    public void visit(WhileStmt whileStmt, ScopeHolder<PDGNode> scope) {
//        // assert whileStmt.getBegin().isPresent();
//
//        PDGNode whileNode = graph.addNode(
//                String.format("while (%s)", whileStmt.getCondition().toString()),
//                whileStmt
//        );
//
//        graph.addControlDependencyArc(scope.getRoot(), whileNode);
//
//        ScopeHolder<PDGNode> whileScope = new ScopeHolder<>(whileNode);
//
//        new VariableExtractor()
//                .setOnVariableUseListener(variable -> {
//                    whileScope.addVariableUse(variable, whileNode);
//
//                    Scope<PDGNode> searchScope = scope.isVariableDefined(variable) ? scope : globalScope;
//
//                    searchScope.getLastDefinitions(variable)
//                            .forEach(variableDefinition -> graph.addDataDependencyArc(
//                                    variableDefinition.getNode(),
//                                    whileNode,
//                                    variable
//                            ));
//                })
//                .visit(whileStmt.getCondition());
//
//        whileStmt.getBody().accept(this, whileScope);
//
//        buildLoopDataDependencies(whileScope);
//
//        scope.addSubscope(whileScope);
//    }
//
//    private void buildLoopDataDependencies(ScopeHolder<PDGNode> scope) {
//        scope.getDefinedVariables()
//                .forEach(variable -> {
//                    List<VariableDefinition<PDGNode>> firstDef = scope.getFirstDefinitions(variable);
//                    List<VariableDefinition<PDGNode>> lastDef = scope.getLastDefinitions(variable);
//
//                    Set<Integer> usesFromLastDef = new HashSet<>();
//
//                    firstDef.forEach(variableDefinition -> {
//                        scope.getVariableUsesBeforeNode(variable, variableDefinition.getNode())
//                                .forEach(use -> {
//                                    if (!usesFromLastDef.contains(use.getNode().getId())) {
//                                        lastDef.forEach(def -> graph.addDataDependencyArc(
//                                                def.getNode(),
//                                                use.getNode(),
//                                                variable)
//                                        );
//
//                                        usesFromLastDef.add(use.getNode().getId());
//                                    }
//                                });
//                    });
//        });
//    }

//    @Override
//    public void visit(ForStmt forStmt, PDGNode node) {
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

//    @Override
//    public void visit(ForEachStmt forEachStmt, ScopeHolder<PDGNode> scope) {
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


//    }

//    @Override
//    public void visit(SwitchStmt switchStmt, PDGNode node) {
//        PDGNode switchNode = graph.addNode(switchStmt.toString());
//
//        graph.addControlDependencyArc(parent, switchNode);
//
//        switchStmt.getSelector().accept(this, parent);
//        switchStmt.getEntries()
//                .forEach(switchEntryStmt -> switchEntryStmt.accept(this, switchNode));
//    }
}
