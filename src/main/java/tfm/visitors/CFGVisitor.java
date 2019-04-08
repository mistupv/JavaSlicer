package tfm.visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.nodes.CFGNode;
import tfm.utils.Logger;

import java.util.*;

public class CFGVisitor extends VoidVisitorAdapter<Void> {

    private CFGGraph graph;

    private Queue<CFGNode> lastParentNodes;

    public CFGVisitor(CFGGraph graph) {
        this.graph = graph;
        this.lastParentNodes = Collections.asLifoQueue(
                new ArrayDeque<>(
                        Collections.singletonList(graph.getRootNode())
                )
        );
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, Void arg) {
        CFGNode nextNode = addNodeAndArcs(expressionStmt.toString(), expressionStmt.getBegin().get().line);

        lastParentNodes.add(nextNode);

        Logger.log(expressionStmt);

        super.visit(expressionStmt, arg);
    }

//    @Override
//    public void visit(VariableDeclarationExpr variableDeclarationExpr, Void arg) {
//        CFGNode<String> nextNode = addNodeAndArcs(variableDeclarationExpr.toString());
//
//        lastParentNodes.add(nextNode);
//
//        Logger.log(variableDeclarationExpr);
//
//        super.visit(variableDeclarationExpr, arg);
//    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        CFGNode ifCondition = addNodeAndArcs(
                String.format("if (%s)", ifStmt.getCondition().toString()),
                ifStmt.getBegin().get().line
        );

        lastParentNodes.add(ifCondition);

        // Visit "then"
        super.visit(blockStmtWrapper(ifStmt.getThenStmt()), arg);

        Queue<CFGNode> lastThenNodes = new ArrayDeque<>(lastParentNodes);

        if (ifStmt.hasElseBranch()) {
            lastParentNodes.clear();
            lastParentNodes.add(ifCondition); // Set if nodes as root

            super.visit(ifStmt.getElseStmt().get().asBlockStmt(), arg);

            lastParentNodes.addAll(lastThenNodes);
        } else {
            lastParentNodes.add(ifCondition);
        }
    }

    @Override
    public void visit(WhileStmt whileStmt, Void arg) {
        CFGNode whileCondition = addNodeAndArcs(
                String.format("while (%s)", whileStmt.getCondition().toString()),
                whileStmt.getBegin().get().line
        );

        lastParentNodes.add(whileCondition);

        super.visit(whileStmt.getBody().asBlockStmt(), arg);

        while (!lastParentNodes.isEmpty()) {
            graph.addControlFlowEdge(lastParentNodes.poll(), whileCondition);
        }

        lastParentNodes.add(whileCondition);
    }

    @Override
    public void visit(ForStmt forStmt, Void arg) {
        forStmt.getInitialization().forEach(expression -> new ExpressionStmt(expression).accept(this, null));

        BlockStmt blockStatement = blockStmtWrapper(forStmt.getBody());

        forStmt.getUpdate().forEach(blockStatement::addStatement);

        Expression comparison = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));

        visit(new WhileStmt(comparison, blockStatement), null);
    }

    @Override
    public void visit(ForEachStmt forEachStmt, Void arg) {
        // init
        NodeList<Expression> initialization = new NodeList<>();
        // Iterable iterable = var.getClass().isArray() ? Arrays.asList(var) : (Iterable) (Object) var;
//        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(
//                new ClassOrInterfaceType("Iterable"),
//                Utils.wrapIntoList(
//                        new VariableDeclarator(
//                                new VariableDeclaratorId("iterable"),
//                                new ConditionalExpr(
//                                        new MethodCallExpr(
//                                                new MethodCallExpr(foreachStmt.getIterable(), "getClass"),
//                                                "isArray"
//                                        ),
//                                        new MethodCallExpr(
//                                                new NameExpr("Arrays"),
//                                                "asList",
//                                                Utils.wrapIntoList(foreachStmt.getIterable())
//                                        ),
//                                        new CastExpr(
//                                                new ClassOrInterfaceType("Iterable"),
//                                                new CastExpr(
//                                                        new ClassOrInterfaceType("Object"),
//                                                        foreachStmt.getIterable()
//                                                )
//                                        )
//                                )
//                        )
//                )
//        );

//        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(
//                new VariableDeclarator(
//                        JavaParser.parseClassOrInterfaceType("Iterator"),
//                        "iterator",
//                        new ConditionalExpr(
//                                new MethodCallExpr(
//                                        new MethodCallExpr(
//                                                forEachStmt.getVariable().getVariables().get(0).getNameAsExpression(),
//                                                "getClass"
//                                        ),
//                                        "isArray"
//                                ),
//                                new MethodCallExpr(
//                                        new MethodCallExpr(
//                                                new NameExpr("Arrays"),
//                                                "asList",
//                                                forEachStmt.getVariable().getVariables().get(0).getNameAsExpression()
//                                        ),
//                                        "iterator"
//                                ),
//
//                        )
//                )
//        );

//        initialization.add(variableDeclarationExpr);

        // condition
        Expression condition =
                new MethodCallExpr(
                        new NameExpr("iterator"),
                        "hasNext"
                );

        BlockStmt body = blockStmtWrapper(forEachStmt.getBody());
        NodeList<Statement> stmts = body.getStatements();
        stmts.addFirst(
            new ExpressionStmt(
                new VariableDeclarationExpr(
                    new VariableDeclarator(
                            forEachStmt.getVariable().getCommonType(),
                            forEachStmt.getVariable().getVariables().get(0).getNameAsString(),
                            new CastExpr(forEachStmt.getVariable().getCommonType(),
                                new MethodCallExpr(
                                    new NameExpr("iterator"),
                                    "next"
                                )
                            )
                    )
                )
            )
        );

        visit(new ForStmt(initialization, condition, new NodeList<>(), body), null);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        super.visit(methodDeclaration, arg);

        addNodeAndArcs("Stop", methodDeclaration.getBegin().get().line);
    }

    private CFGNode addNodeAndArcs(String nodeData, int fileNumber) {
        CFGNode node = graph.addNode(nodeData, fileNumber);

        CFGNode parent = lastParentNodes.poll(); // ALWAYS exists a parent
        graph.addControlFlowEdge(parent, node);

        while (!lastParentNodes.isEmpty()) {
            parent = lastParentNodes.poll();
            graph.addControlFlowEdge(parent, node);
        }

        return node;
    }

    private BlockStmt blockStmtWrapper(Statement node) {
        if (node.isBlockStmt()) {
            return (BlockStmt) node;
        }

        NodeList<Statement> nodeList = new NodeList<>();
        nodeList.add(node);
        return new BlockStmt(nodeList);
    }
}
