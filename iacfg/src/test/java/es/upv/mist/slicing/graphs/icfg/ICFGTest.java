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

        createGraph("TestInicial.java", "grafoInicial");
        System.out.println("Grafo 1 generado...");

        createGraph("TestGlobalVariables.java", "grafoGlobalVariables");
        System.out.println("Grafo 2 generado...");

        createGraph("TestEmbebedFunctions.java", "grafoEmbebedFunctions");
        System.out.println("Grafo 3 generado...");

        createGraph("TestInlineFunctions.java", "grafoInlineVariables");
        System.out.println("Grafo 4 generado...");

        createGraph("TestExamplePaper.java", "grafoPaper");
        System.out.println("Grafo 5 generado...");

        createGraph("TestExamplePaperSimple.java", "grafoPaperSimple");
        System.out.println("Grafo 6 generado...");
    }

    private static void createGraph(String fileName, String graphName) throws IOException {
        File file = new File(Thread.currentThread().getContextClassLoader().getResource(fileName).getPath());
        NodeList<CompilationUnit> units = new NodeList<>();
        try {
            units.add(StaticJavaParser.parse(file));
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró el archivo");
        }
        ICFG icfg = new ICFG();
        icfg.build(units);
        new ICFGLog(icfg).generateImages(graphName);
    }
}
