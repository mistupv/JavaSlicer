package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESPDG;

import java.util.Set;

public class JSysPDG extends ESPDG {
    public JSysPDG(ClassGraph classGraph, Set<ConstructorDeclaration> implicitConstructors) {
        this(new JSysCFG(classGraph, implicitConstructors));
    }

    public JSysPDG(JSysCFG cfg) {
        super(cfg);
    }
}
