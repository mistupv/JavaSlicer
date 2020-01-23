package tfm.exec;

import tfm.graphs.PDG;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PDGLog extends GraphLog<PDG> {
    private CFGLog cfgLog;

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
    public void log() throws IOException {
        super.log();

        Logger.log("Nodes with variable info");
        Logger.log(graph.vertexSet().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .map(node ->
                        String.format("GraphNode { id: %s, declared: %s, defined: %s, used: %s }",
                                node.getId(),
                                node.getDeclaredVariables(),
                                node.getDefinedVariables(),
                                node.getUsedVariables())
                ).collect(Collectors.joining(System.lineSeparator()))
        );
    }

    @Override
    public void generateImages(String imageName, Format format) throws IOException {
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
