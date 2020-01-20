package tfm.exec;

import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;
import tfm.visitors.pdg.PDGBuilder;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PDGLog extends GraphLog<PDGGraph> {

    private CFGLog cfgLog;

    public PDGLog() {
        this(null);
    }

    public PDGLog(PDGGraph pdgGraph) {
        super(pdgGraph);

        if (graph != null && graph.getCfgGraph() != null)
            cfgLog = new CFGLog(graph.getCfgGraph());
        else cfgLog = null;
    }

    @Override
    public void visit(com.github.javaparser.ast.Node node) {
        this.graph = new PDGGraph();

        node.accept(new PDGBuilder(graph), null);

        if (cfgLog == null) {
            cfgLog = new CFGLog(graph.getCfgGraph());
        }
    }

    @Override
    public void log() throws IOException {
        super.log();

        Logger.log("Nodes with variable info");
        Logger.log(graph.getNodes().stream()
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
        super.generateImages(imageName + "-pdg", format);
        if (cfgLog != null)
            cfgLog.generateImages(imageName + "-cfg", format);
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        super.openVisualRepresentation();

        if (cfgLog != null)
            cfgLog.openVisualRepresentation();
    }
}
