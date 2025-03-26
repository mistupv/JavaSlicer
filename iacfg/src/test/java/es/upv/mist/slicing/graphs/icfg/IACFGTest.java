package es.upv.mist.slicing.graphs.icfg;

import es.upv.mist.slicing.I_ACFG;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.cli.DOTAttributes;
import es.upv.mist.slicing.cli.GraphLog;

import java.io.File;
import java.io.IOException;

public class IACFGTest {
    public static void main(String[] args) throws IOException {
        File file = new File(Thread.currentThread().getContextClassLoader().getResource("Test.java").getPath());

        I_ACFG icfg = new I_ACFG();
        icfg.build(file);
        new GraphLog<>(icfg) {
            @Override
            protected DOTAttributes edgeAttributes(Arc arc) {
                DOTAttributes att = super.edgeAttributes(arc);
                if (arc.isNonExecutableControlFlowArc())
                    att.add("style", "dashed");
                return att;
            }
        }.generateImages("migrafo");
        System.out.println("Grafo generado...");
    }
}
