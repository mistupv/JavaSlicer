package es.upv.mist.slicing.graphs.icfg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import es.upv.mist.slicing.utils.StaticTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ICFGTest {
    public static void main(String[] args) throws IOException {
        StaticJavaParser.getConfiguration().setAttributeComments(false);
        StaticTypeSolver.addTypeSolverJRE();

        File file = new File(Thread.currentThread().getContextClassLoader().getResource("Test.java").getPath());
        NodeList<CompilationUnit> units = new NodeList<>();
        try {
            units.add(StaticJavaParser.parse(file));
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró el archivo");
        }
        ICFG icfg = new ICFG();
        icfg.build(units);
        new ICFGLog(icfg).generateImages("migrafo");
        System.out.println("Grafo generado...");
    }
}
