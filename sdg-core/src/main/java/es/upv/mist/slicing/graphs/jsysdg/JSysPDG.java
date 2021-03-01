package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.graphs.exceptionsensitive.ESPDG;

public class JSysPDG extends ESPDG {
    public JSysPDG() {
        this(new JSysCFG());
    }

    public JSysPDG(JSysCFG cfg) {
        super(cfg);
    }
}
