package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESPDG;

public class JSysPDG extends ESPDG {
    public JSysPDG(ClassGraph clg) {
        this(new JSysCFG(clg));
    }

    public JSysPDG(JSysCFG cfg) {
        super(cfg);
    }
}
