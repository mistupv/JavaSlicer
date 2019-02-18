package tfm.graphlib;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import edg.graphlib.Graph;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("/home/jacosro/IdeaProjects/TFM/src/main/java/tfm/programs/Example2.java");
        CompilationUnit compilationUnit = JavaParser.parse(file);

        Graph graph = new Graph();
    }
}
