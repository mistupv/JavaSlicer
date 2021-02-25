package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.LinkedList;
import java.util.List;

public class JSysCFG extends ESCFG {

    /** ClassGraph associated to the Method represented by the CFG */
    protected ClassGraph clg;

    public JSysCFG(ClassGraph clg){
        super();
        this.clg = clg;
    }

    public ClassGraph getClassGraph(){
        return this.clg;
    }

    protected CFGBuilder newCFGBuilder() {
        return new Builder(this);
    }

    /** Obtains the Javaparser Node corresponding to the class where the CFG is contained */
    public ClassOrInterfaceDeclaration getDeclarationClass() {
        assert rootNode != null;
        if (!(rootNode.getAstNode().getParentNode().get() instanceof ClassOrInterfaceDeclaration))
            throw new IllegalStateException("The Method declaration is not directly inside a Class Declaration");
        return (ClassOrInterfaceDeclaration) rootNode.getAstNode().getParentNode().get();
    }

    public class Builder extends ESCFG.Builder {

        /** List of inserted super calls in Javaparser AST to process them as Implicit Nodes (@ImplicitNode)*/
        protected List<Node> methodInsertedInstructions = new LinkedList<>();

        protected Builder(JSysCFG jSysCFG) {
            super(JSysCFG.this);
            assert jSysCFG == JSysCFG.this;
        }

        /** Esto se llama porque lo hemos insertado fantasma o porque existe. A continuacion se inserta el codigo dynInit */
        @Override
        public void visit(ExplicitConstructorInvocationStmt n, Void arg) {

            // 1. Create new super call if not present
            if (methodInsertedInstructions.contains(n)){
                ImplicitNode node = new ImplicitNode(n.toString(), n);
                graph.addVertex(node);
                connectTo(node);
            }
            else {
                connectTo(n);
            }
            // 2. Insert dynamic class code
            ClassOrInterfaceDeclaration containerClass = ((JSysCFG) graph).getDeclarationClass();
            List<BodyDeclaration<?>> dynInitList = ((JSysCFG) graph).getClassGraph().getDynInit(containerClass.getNameAsString());
            dynInitList.forEach(node -> node.accept(this, arg));

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
                methodInsertedInstructions.add(superCall);
                n.getBody().addStatement(0, superCall);
            }
            // Perform the same task as previous graphs.
            super.visit(n, arg);
        }
    }
}
