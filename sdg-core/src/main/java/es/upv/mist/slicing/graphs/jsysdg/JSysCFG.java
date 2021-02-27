package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * An SDG that is tailored for Java, including a class graph, inheritance,
 * polymorphism and other features.
 */
public class JSysCFG extends ESCFG {
    /** ClassGraph associated to the Method represented by the CFG */
    protected ClassGraph classGraph;
    /** Set of constructors that must be built with implicit nodes. */
    protected Set<ConstructorDeclaration> implicitConstructors;

    public JSysCFG(ClassGraph classGraph, Set<ConstructorDeclaration> implicitConstructors) {
        super();
        this.classGraph = classGraph;
        this.implicitConstructors = implicitConstructors;
    }

    @Override
    public void buildRootNode(CallableDeclaration<?> rootNodeAst) {
        super.buildRootNode(rootNodeAst);
        if (implicitConstructors.contains(rootNodeAst))
            rootNode.markAsImplicit();
    }

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new Builder(this);
    }

    public class Builder extends ESCFG.Builder {
        /** List of implicit instructions inserted explicitly in this CFG.
         *  They should be included in the graph as ImplicitNodes. */
        protected List<Node> methodInsertedInstructions = new LinkedList<>();
        /** Whether we are building a CFG for an implicit method or not. */
        protected boolean implicitDeclaration = false;

        protected Builder(JSysCFG jSysCFG) {
            super(JSysCFG.this);
            assert jSysCFG == JSysCFG.this;
        }

        @Override
        protected <T extends Node> GraphNode<T> connectTo(T n, String text) {
            GraphNode<T> dest;
            dest = new GraphNode<>(text, n);
            if (methodInsertedInstructions.contains(n) ||
                    (implicitDeclaration && !(n instanceof FieldDeclaration)))
                dest.markAsImplicit();
            addVertex(dest);
            connectTo(dest);
            return dest;
        }

        @Override
        public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
            // 1. Connect to the following statements
            connectTo(n);
            // 2. Insert dynamic class code (only for super())
            if (!n.isThis()) {
                ClassOrInterfaceDeclaration containerClass = ASTUtils.getClassNode(rootNode.getAstNode());
                classGraph.getDynInit(containerClass.getNameAsString()).forEach(node -> node.accept(this, arg));
            }
            // 3. Handle exceptions
            super.visitCallForExceptions(n);
        }

        @Override
        public void visit(FieldDeclaration n, Void arg){
            connectTo(n);
            super.visit(n,arg);
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            if (implicitConstructors.contains(n))
                implicitDeclaration = true;
            // Insert call to super() if it is implicit.
            if (!ASTUtils.constructorHasExplicitConstructorInvocation(n)){
                var superCall = new ExplicitConstructorInvocationStmt(null, null, false, null, new NodeList<>());
                var returnThis = new ReturnStmt(new ThisExpr());
                methodInsertedInstructions.add(superCall);
                methodInsertedInstructions.add(returnThis);
                n.getBody().addStatement(0, superCall);
                n.getBody().addStatement(returnThis);
            }
            // Perform the same task as previous graphs.
            super.visit(n, arg);
            // Convert the exit nodes to implicit if appropriate
            if (implicitDeclaration)
                vertexSet().stream()
                        .filter(MethodExitNode.class::isInstance)
                        .forEach(GraphNode::markAsImplicit);
        }
    }
}
