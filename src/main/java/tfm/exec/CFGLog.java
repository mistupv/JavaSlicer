package tfm.exec;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import tfm.graphs.CFGGraph;
import tfm.graphs.Graph;
import tfm.graphs.PDGGraph;
import tfm.visitors.CFGVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CFGLog extends GraphLog<CFGGraph> {

    public CFGLog() {

    }

    public CFGLog(CFGGraph graph) {
        this.graph = graph;
    }

    @Override
    public void visit(Node node) {
        this.graph = new CFGGraph();

        node.accept(new CFGVisitor(graph), null);
    }

    @Override
    public void generatePNGs() throws IOException {
        this.generatePNGs("cfg");
    }

    @Override
    public void generatePNGs(String pngName) throws IOException {
        Graphviz.fromString(graph.toGraphvizRepresentation())
                .render(Format.PNG)
                .toFile(new File("./out/" + pngName + ".png"));
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        new ProcessBuilder(Arrays.asList("xdg-open", "./out/cfg.png")).start();
    }
}
