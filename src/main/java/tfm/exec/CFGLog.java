package tfm.exec;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import tfm.graphs.CFGGraph;
import tfm.graphs.Graph;
import tfm.visitors.CFGVisitor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CFGLog extends GraphLog<CFGGraph, CFGVisitor> {

    @Override
    public void visit(Node node) {
        this.graph = new CFGGraph() {
            @Override
            protected String getRootNodeData() {
                return "Start";
            }
        };

        this.visitor = new CFGVisitor(graph);

        node.accept(visitor, null);
    }

    @Override
    void generatePNGs() throws IOException {
        Graphviz.fromString(graph.toGraphvizRepresentation())
                .render(Format.PNG)
                .toFile(new File("./out/cfg.png"));
    }

    @Override
    public void openVisualRepresentation() throws IOException {
        new ProcessBuilder(Arrays.asList("xdg-open", "./out/cfg.png")).start();
    }
}
