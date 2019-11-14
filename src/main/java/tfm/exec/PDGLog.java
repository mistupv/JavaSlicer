package tfm.exec;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;
import tfm.visitors.pdg.PDGBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PDGLog extends GraphLog<PDGGraph> {

    public PDGLog() {
        super();
    }

    public PDGLog(PDGGraph pdgGraph) {
        super(pdgGraph);
    }

    @Override
    public void visit(com.github.javaparser.ast.Node node) {
        this.graph = new PDGGraph();

        node.accept(new PDGBuilder(graph), this.graph.getRootNode());
    }

    @Override
    public void log() throws IOException {
        super.log();

        Logger.log("Nodes with variable info");
        Logger.log(
                graph.getNodes().stream()
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
    public void generatePNGs() throws IOException {
        this.generatePNGs("program");
    }

    @Override
    public void generatePNGs(String pngName) throws IOException {
        this.pngName = pngName;

        if (graph.getCfgGraph() != null) {
            Graphviz.fromString(this.graph.getCfgGraph().toGraphvizRepresentation())
                    .render(Format.PNG)
                    .toFile(new File("./out/" + pngName + "-cfg.png"));
        }

        Graphviz.fromString(graph.toGraphvizRepresentation())
                .render(Format.PNG)
                .toFile(new File("./out/" + pngName + "-pdg.png"));
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        if (this.graph.getCfgGraph() != null) {
            new ProcessBuilder(Arrays.asList("xdg-open", "./out/" + pngName + "-cfg.png")).start();
        }

        new ProcessBuilder(Arrays.asList("xdg-open", "./out/" + pngName + "-pdg.png")).start();
    }
}
