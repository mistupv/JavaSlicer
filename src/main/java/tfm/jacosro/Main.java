package tfm.jacosro;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import tfm.jacosro.graphs.CFGGraph;
import tfm.jacosro.visitors.CFGVisitor;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("/home/jacosro/IdeaProjects/TFM/src/main/java/tfm/programs/Example2.java");
        CompilationUnit compilationUnit = JavaParser.parse(file);

        CFGGraph<String> graph = new CFGGraph<String>() {
            @Override
            protected String getStartNodeData() {
                return "Start";
            }
        };

        CFGVisitor visitor = new CFGVisitor(graph);

        visitor.visit(compilationUnit, null);

        System.out.println(graph);
    }
}
