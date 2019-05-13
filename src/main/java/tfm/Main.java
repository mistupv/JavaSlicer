package tfm;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import edg.graphlib.Vertex;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import tfm.graphs.CFGGraph;
import tfm.graphs.Graph;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGNode;
import tfm.scopes.ScopeHolder;
import tfm.utils.Logger;
import tfm.visitors.CFGVisitor;
import tfm.visitors.PDGVisitor;

import java.io.*;
import java.util.Arrays;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;

public class Main {

    private static long t0;

    public static void main(String[] args) throws IOException, InterruptedException {
        File file = new File("/home/jacosro/IdeaProjects/TFM/src/main/java/tfm/programs/Example1.java");
        CompilationUnit compilationUnit = JavaParser.parse(file);

        t0 = System.nanoTime();

        Graph<?> graph = pdg(file, compilationUnit);

        long tt = System.nanoTime();

        Logger.log(
                "****************************\n" +
                "*           GRAPH          *\n" +
                "****************************"
        );
        Logger.log(graph);
        Logger.log(
                "****************************\n" +
                "*         GRAPHVIZ         *\n" +
                "****************************"
        );
        Logger.log(graph.toGraphvizRepresentation());
        Logger.log();
        Logger.format("Done in %.2f ms", (tt - t0) / 10e6);

        openGraphAsPng(graph);
    }

    public static CFGGraph cfg(File file, CompilationUnit cu) {
        CFGGraph cfgGraph = new CFGGraph() {
            @Override
            protected String getRootNodeData() {
                return "Start";
            }
        };

        cu.accept(new CFGVisitor(cfgGraph), null);

        return cfgGraph;
    }

    public static PDGGraph pdg(File file, CompilationUnit cu) {
        PDGGraph pdgGraph = new PDGGraph() {
            @Override
            protected String getRootNodeData() {
                return "Entry";
            }
        };

        ScopeHolder<PDGNode> scopeHolder = new ScopeHolder<>(pdgGraph.getRootNode());
        PDGVisitor visitor = new PDGVisitor(pdgGraph, scopeHolder);

        cu.accept(visitor, scopeHolder);

        return pdgGraph;
    }

    private static void openGraphAsPng(Graph graph) throws IOException, InterruptedException {
        PrintWriter printWriter = new PrintWriter("./out/graph.txt");
        printWriter.println(graph.toGraphvizRepresentation());
        printWriter.close();

        int exit = new ProcessBuilder(
                Arrays.asList("dot", "-Tpng", "./out/graph.txt", "-o", "./out/graph.png")
        ).start()
        .waitFor();

        if (exit != 0) {
            Logger.log("Error procesando el archivo de grafo");
            return;
        }

        new ProcessBuilder(Arrays.asList("xdg-open", "./output/graph.png")).start();
    }
}
