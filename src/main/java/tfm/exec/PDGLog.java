package tfm.exec;

import guru.nidi.graphviz.engine.Format;
import tfm.graphs.PDG;
import tfm.graphs.PDG.APDG;
import tfm.graphs.PDG.PPDG;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;
import tfm.visitors.pdg.PDGBuilder;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PDGLog extends GraphLog<PDG> {
    public static final int PDG = 0, APDG = 1, PPDG = 2;

    private CFGLog cfgLog;
    private int type;

    public PDGLog(int type) {
        this(null);
        this.type = type;
    }

    public PDGLog(PDG pdg) {
        super(pdg);

        if (graph != null && graph.getCfg() != null)
            cfgLog = new CFGLog(graph.getCfg());
        else cfgLog = null;
    }

    @Override
    public void visit(com.github.javaparser.ast.Node node) {
        switch (type) {
            case PDG:
                this.graph = new PDG();
                break;
            case APDG:
                this.graph = new APDG();
                break;
            case PPDG:
                this.graph = new PPDG();
                break;
            default:
                throw new RuntimeException("Invalid type of PDG");
        }

        node.accept(new PDGBuilder(graph), this.graph.getRootNode());

        if (cfgLog == null) {
            cfgLog = new CFGLog(graph.getCfg());
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
        super.generateImages(imageName + "-" + getExtra(), format);
        if (cfgLog != null)
            cfgLog.generateImages(imageName + "-cfg", format);
    }

    private String getExtra() {
        if (graph instanceof PPDG)
            return "ppdg";
        else if (graph instanceof APDG)
            return "apdg";
        else if (graph instanceof PDG)
            return "pdg";
        throw new RuntimeException("invalid or null graph type");
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        super.openVisualRepresentation();

        if (cfgLog != null)
            cfgLog.openVisualRepresentation();
    }
}
