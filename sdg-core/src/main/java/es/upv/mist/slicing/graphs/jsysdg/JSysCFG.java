package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG;

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
        return new JSysCFGBuilder(this);
    }


    /** Obtains the Javaparser Node corresponding to the class where the CFG is contained */
    public ClassOrInterfaceDeclaration getDeclarationClass() {
        assert rootNode != null;
        if (!(rootNode.getAstNode().getParentNode().get() instanceof ClassOrInterfaceDeclaration))
            throw new IllegalStateException("The Method declaration is not directly inside a Class Declaration");
        return (ClassOrInterfaceDeclaration) rootNode.getAstNode().getParentNode().get();
    }
}
