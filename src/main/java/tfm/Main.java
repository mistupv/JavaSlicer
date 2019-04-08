package tfm;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import tfm.graphs.CFGGraph;
import tfm.graphs.Graph;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGNode;
import tfm.utils.Logger;
import tfm.visitors.CFGVisitor;
import tfm.visitors.PDGVisitor;

import java.io.File;
import java.io.FileNotFoundException;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;

public class Main {

    private static long t0;

    public static void main(String[] args) throws FileNotFoundException {
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

        VoidVisitor<PDGNode> voidVisitor = new PDGVisitor(pdgGraph);

        cu.accept(voidVisitor, pdgGraph.getRootNode());

        return pdgGraph;
    }
}
