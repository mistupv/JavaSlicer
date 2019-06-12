//package tfm.visitors;
//
//import com.github.javaparser.ast.stmt.ExpressionStmt;
//import com.github.javaparser.ast.stmt.IfStmt;
//import com.github.javaparser.ast.stmt.Statement;
//import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
//import edg.graphlib.Arrow;
//import org.checkerframework.checker.nullness.qual.NonNull;
//import tfm.arcs.cfg.ControlFlowArc;
//import tfm.graphs.CFGGraph;
//import tfm.graphs.PDGGraph;
//import tfm.nodes.CFGNode;
//import tfm.nodes.PDGNode;
//import tfm.variables.VariableExtractor;
//
//import java.util.Optional;
//
//public class PDGCFGVisitor extends VoidVisitorAdapter<PDGNode> {
//
//    private CFGGraph cfgGraph;
//    private PDGGraph pdgGraph;
//
//    private CFGVisitor cfgVisitor;
//
//    public PDGCFGVisitor(PDGGraph pdgGraph) {
//        this(pdgGraph, new CFGGraph() {
//            @Override
//            protected String getRootNodeData() {
//                return "Start";
//            }
//        });
//    }
//
//    public PDGCFGVisitor(PDGGraph pdgGraph, CFGGraph cfgGraph) {
//        this.pdgGraph = pdgGraph;
//        this.cfgGraph = cfgGraph;
//        this.cfgVisitor = new CFGVisitor(cfgGraph);
//    }
//
//    @Override
//    public void visit(ExpressionStmt expressionStmt, PDGNode parent) {
//        buildCFG(expressionStmt);
//
//        PDGNode node = pdgGraph.addNode(expressionStmt.toString(), expressionStmt);
//        pdgGraph.addControlDependencyArc(node, parent);
//
//        new VariableExtractor()
//                .setOnVariableUseListener(variable -> {
//                    Optional<CFGNode> nodeOptional = cfgGraph.findNodeByStatement(expressionStmt);
//
//                    if (!nodeOptional.isPresent()) {
//                        return;
//                    }
//
//                    CFGNode cfgNode = nodeOptional.get();
//
//                    findLastDefinitionsFrom(cfgNode, variable);
//                })
//                .visit(expressionStmt);
//    }
//
//    private void findLastDefinitionsFrom(CFGNode cfgNode, String variable) {
//        for (Arrow arrow : cfgNode.getIncomingArrows()) {
//            ControlFlowArc controlFlowArc = (ControlFlowArc) arrow;
//
//
//        }
//    }
//
//    @Override
//    public void visit(IfStmt ifStmt, PDGNode parent) {
//
//    }
//
//    private void buildCFG(Statement statement) {
//        statement.accept(this.cfgVisitor, null);
//    }
//}
