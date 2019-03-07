package tfm.graphlib;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.graphs.CFGGraph;
import tfm.graphlib.graphs.PDGGraph;
import tfm.graphlib.nodes.PDGVertex;
import tfm.graphlib.visitors.CFGVisitor;
import tfm.graphlib.visitors.PDGVisitor;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("/home/jacosro/IdeaProjects/TFM/src/main/java/tfm/programs/Example1.java");
        CompilationUnit compilationUnit = JavaParser.parse(file);

//        CFGGraph cfgGraph = new CFGGraph() {
//            @Override
//            protected String getRootNodeData() {
//                return "Start";
//            }
//        };
        PDGGraph pdgGraph = new PDGGraph() {
            @Override
            protected String getRootNodeData() {
                return "Entry";
            }
        };

        VoidVisitor<PDGVertex> voidVisitor = new PDGVisitor(pdgGraph);

        compilationUnit.accept(voidVisitor, pdgGraph.getRootVertex());
        // compilationUnit.accept(new PDGVisitor(pdgGraph), pdgGraph.getRootVertex());

        System.out.println(pdgGraph);
//        System.out.println(cfgGraph.toGraphvizRepresentation());
    }
}
