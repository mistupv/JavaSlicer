package tfm.graphs.pdg;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.VoidType;
import tfm.graphs.augmented.ACFG;
import tfm.graphs.augmented.APDG;
import tfm.graphs.augmented.PPDG;
import tfm.nodes.GraphNode;

public class HandCraftedGraphs {
    public static APDG problem1WithGotos() {
        // Generate the control flow of a graph
        ACFG cfg = new ACFG();
        cfg.buildRootNode("ENTER Problem1", new MethodDeclaration(new NodeList<>(), new VoidType(), "Problem1"));
        GraphNode<?> wx = cfg.addNode("while (X)", new WhileStmt());
        GraphNode<?> ify = cfg.addNode("L: if (Y)", new IfStmt());
        GraphNode<?> ifz = cfg.addNode("if (Z)", new IfStmt());
        GraphNode<?> a = cfg.addNode("A();", new MethodCallExpr("A"));
        GraphNode<?> b = cfg.addNode("B();", new MethodCallExpr("B"));
        GraphNode<?> c = cfg.addNode("C();", new MethodCallExpr("C"));
        GraphNode<?> d = cfg.addNode("D();", new MethodCallExpr("D"));
        GraphNode<?> g1 = cfg.addNode("goto L;", new ContinueStmt("L"));
        GraphNode<?> g2 = cfg.addNode("goto L;", new ContinueStmt("L"));

        GraphNode<?> end = cfg.addNode("Exit", new EmptyStmt());

        cfg.addControlFlowEdge(cfg.getRootNode().get(), wx);
        cfg.addControlFlowEdge(wx, ify);
        cfg.addControlFlowEdge(wx, d);
        cfg.addControlFlowEdge(ify, ifz);
        cfg.addControlFlowEdge(ify, c);
        cfg.addControlFlowEdge(ifz, a);
        cfg.addControlFlowEdge(ifz, b);
        cfg.addControlFlowEdge(a, g1);
        cfg.addControlFlowEdge(b, g2);
        cfg.addControlFlowEdge(c, wx);
        cfg.addControlFlowEdge(d, end);
        cfg.addNonExecutableControlFlowEdge(g1, b);
        cfg.addControlFlowEdge(g1, ify);
        cfg.addNonExecutableControlFlowEdge(g2, c);
        cfg.addControlFlowEdge(g2, ify);
        cfg.addNonExecutableControlFlowEdge(cfg.getRootNode().get(), end);

        PPDG pdg = new PPDG(cfg);
        ControlDependencyBuilder gen = new ControlDependencyBuilder(pdg, cfg);
        gen.analyze();
        return pdg;
    }

    public static APDG problem1ContinueWithGotos() {
        // Generate the control flow of a graph
        ACFG cfg = new ACFG();
        cfg.buildRootNode("ENTER Problem1", new MethodDeclaration(new NodeList<>(), new VoidType(), "Problem1"));
        GraphNode<?> wx = cfg.addNode("while (X)", new WhileStmt());
        GraphNode<?> ify = cfg.addNode("L: if (Y)", new IfStmt());
        GraphNode<?> ifz = cfg.addNode("if (Z)", new IfStmt());
        GraphNode<?> a = cfg.addNode("A();", new MethodCallExpr("A"));
        GraphNode<?> b = cfg.addNode("B();", new MethodCallExpr("B"));
        GraphNode<?> c = cfg.addNode("C();", new MethodCallExpr("C"));
        GraphNode<?> d = cfg.addNode("D();", new MethodCallExpr("D"));
        GraphNode<?> g1 = cfg.addNode("goto L1;", new ContinueStmt("L"));
        GraphNode<?> g2 = cfg.addNode("goto L2;", new ContinueStmt("L"));
        GraphNode<?> g3 = cfg.addNode("goto L3;", new ContinueStmt("L"));

        GraphNode<?> end = cfg.addNode("Exit", new EmptyStmt());

        cfg.addControlFlowEdge(cfg.getRootNode().get(), wx);
        cfg.addControlFlowEdge(wx, ify);
        cfg.addControlFlowEdge(wx, d);
        cfg.addControlFlowEdge(ify, ifz);
        cfg.addControlFlowEdge(ify, c);
        cfg.addControlFlowEdge(ifz, a);
        cfg.addControlFlowEdge(ifz, b);
        cfg.addControlFlowEdge(a, g1);
        cfg.addControlFlowEdge(b, g3);
        cfg.addControlFlowEdge(c, wx);
        cfg.addControlFlowEdge(d, end);
        cfg.addNonExecutableControlFlowEdge(g1, b);
        cfg.addControlFlowEdge(g1, ify);
        cfg.addNonExecutableControlFlowEdge(g2, c);
        cfg.addControlFlowEdge(g2, ify);
        cfg.addNonExecutableControlFlowEdge(g3, g2);
        cfg.addControlFlowEdge(g3, ify);
        cfg.addNonExecutableControlFlowEdge(cfg.getRootNode().get(), end);

        PPDG pdg = new PPDG(cfg);
        ControlDependencyBuilder gen = new ControlDependencyBuilder(pdg, cfg);
        gen.analyze();
        return pdg;
    }
}
