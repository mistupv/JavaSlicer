package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
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
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * An SDG that is tailored for Java, including a class graph, inheritance,
 * polymorphism and other features.
 */
public class JSysCFG extends ESCFG {
    /** ClassGraph associated to the Method represented by the CFG */
    protected ClassGraph classGraph;

    public JSysCFG(ClassGraph classGraph){
        super();
        this.classGraph = classGraph;
    }

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new Builder(this);
    }

    public class Builder extends ESCFG.Builder {
        /** List of implicit instructions inserted explicitly in this CFG.
         *  They should be included in the graph as ImplicitNodes. */
        protected List<Node> methodInsertedInstructions = new LinkedList<>();

        protected Builder(JSysCFG jSysCFG) {
            super(JSysCFG.this);
            assert jSysCFG == JSysCFG.this;
        }

        @Override
        protected <T extends Node> GraphNode<T> connectTo(T n, String text) {
            GraphNode<T> dest;
            if (methodInsertedInstructions.contains(n)) {
                dest = new ImplicitNode<>(n.toString(), n);
            } else {
                dest = new GraphNode<>(text, n);
            }
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
        }
    }
}
