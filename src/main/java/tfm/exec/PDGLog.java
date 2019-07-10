package tfm.exec;

import com.github.javaparser.ast.CompilationUnit;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import tfm.graphs.PDGGraph;
import tfm.nodes.Node;
import tfm.utils.Logger;
import tfm.visitors.PDGCFGVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PDGLog extends GraphLog<PDGGraph, PDGCFGVisitor> {

    public PDGLog() {
        super();
    }

    public PDGLog(PDGGraph pdgGraph) {
        super(pdgGraph);
    }

    @Override
    public void visit(com.github.javaparser.ast.Node node) {
        this.graph = new PDGGraph();

        this.visitor = new PDGCFGVisitor(graph);

        node.accept(this.visitor, this.graph.getRootNode());
    }

    @Override
    public void log() throws IOException {
        super.log();

        Logger.log("Nodes with variable info");
        Logger.log(
                graph.getNodes().stream()
                        .sorted(Comparator.comparingInt(Node::getId))
                        .map(node ->
                                String.format("Node { id: %s, declared: %s, defined: %s, used: %s }",
                                        node.getId(),
                                        node.getDeclaredVariables(),
                                        node.getDefinedVariables(),
                                        node.getUsedVariables())
                        ).collect(Collectors.joining(System.lineSeparator()))
        );
    }

    @Override
    public void generatePNGs() throws IOException {
        if (visitor != null) {
            Graphviz.fromString(this.visitor.getCfgGraph().toGraphvizRepresentation())
                    .render(Format.PNG)
                    .toFile(new File("./out/pdg-cfg.png"));
        }

        Graphviz.fromString(graph.toGraphvizRepresentation())
                .render(Format.PNG)
                .toFile(new File("./out/pdg.png"));
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        if (visitor != null) {
            new ProcessBuilder(Arrays.asList("xdg-open", "./out/pdg-cfg.png")).start();
        }

        new ProcessBuilder(Arrays.asList("xdg-open", "./out/pdg.png")).start();
    }
}
