package tfm;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import tfm.graphs.CFGGraph;
import tfm.graphs.Graph;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGVertex;
import tfm.visitors.CFGVisitor;
import tfm.visitors.PDGVisitor;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("/home/jacosro/IdeaProjects/TFM/src/main/java/tfm/programs/Example2.java");
        CompilationUnit compilationUnit = JavaParser.parse(file);

        Graph<?> graph = cfg(file, compilationUnit);

        System.out.println(graph);
        System.out.println(graph.toGraphvizRepresentation());
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

        VoidVisitor<PDGVertex> voidVisitor = new PDGVisitor(pdgGraph);

        cu.accept(voidVisitor, pdgGraph.getRootVertex());

        return pdgGraph;
    }
}
