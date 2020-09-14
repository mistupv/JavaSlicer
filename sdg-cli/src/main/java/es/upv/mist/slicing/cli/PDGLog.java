package es.upv.mist.slicing.cli;

import es.upv.mist.slicing.graphs.pdg.PDG;

import java.io.IOException;

public class PDGLog extends GraphLog<PDG> {
    private final CFGLog cfgLog;

    public PDGLog() {
        this(null);
    }

    public PDGLog(PDG pdg) {
        super(pdg);

        if (graph != null && graph.getCfg() != null)
            cfgLog = new CFGLog(graph.getCfg());
        else cfgLog = null;
    }

    @Override
    public void generateImages(String imageName, String format) throws IOException {
        super.generateImages(imageName, format);
        if (cfgLog != null)
            cfgLog.generateImages(imageName, format);
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        super.openVisualRepresentation();

        if (cfgLog != null)
            cfgLog.openVisualRepresentation();
    }
}
