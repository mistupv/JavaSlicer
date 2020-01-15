package tfm.exec;

import guru.nidi.graphviz.engine.Format;
import tfm.graphs.CFG;
import tfm.graphs.CFG.ACFG;
import tfm.graphs.CFG.ECFG;
import tfm.graphs.PDG;
import tfm.graphs.PDG.APDG;
import tfm.graphs.PDG.EPDG;
import tfm.graphs.PDG.EPPDG;
import tfm.graphs.PDG.PPDG;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;
import tfm.visitors.pdg.PDGBuilder;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PDGLog extends GraphLog<PDG> {
    public static final int PDG = 0, APDG = 1, PPDG = 2, EPDG = 3, EPPDG = 4;

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
                this.graph.setCfg(new CFG());
                break;
            case APDG:
                this.graph = new APDG();
                this.graph.setCfg(new ACFG());
                break;
            case PPDG:
                this.graph = new PPDG();
                this.graph.setCfg(new ACFG());
                break;
            case EPDG:
                this.graph = new EPDG();
                this.graph.setCfg(new ECFG());
                break;
            case EPPDG:
                this.graph = new EPPDG();
                this.graph.setCfg(new ECFG());
                break;
            default:
                throw new RuntimeException("Invalid type of PDG");
        }

        node.accept(new PDGBuilder(graph, graph.getCfg()), this.graph.getRootNode());

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
            cfgLog.generateImages(imageName + "-" + getExtraCFG(), format);
    }

    private String getExtraCFG() {
        if (graph.getCfg() instanceof ECFG)
            return "ecfg";
        else if (graph.getCfg() instanceof ACFG)
            return "acfg";
        else if (graph.getCfg() instanceof CFG)
            return "cfg";
        throw new RuntimeException("invalid or null cfg graph type");
    }

    private String getExtra() {
        if (graph instanceof EPPDG)
            return "eppdg";
        else if (graph instanceof EPDG)
            return "epdg";
        else if (graph instanceof PPDG)
            return "ppdg";
        else if (graph instanceof APDG)
            return "apdg";
        else if (graph instanceof PDG)
            return "pdg";
        throw new RuntimeException("invalid or null pdg graph type");
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        super.openVisualRepresentation();

        if (cfgLog != null)
            cfgLog.openVisualRepresentation();
    }
}
