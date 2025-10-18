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

        /*createGraph("TestInicial.java", "grafoInicial");
        System.out.println("Grafo 1 generado...");

        createGraph("TestGlobalVariables.java", "grafoGlobalVariables");
        System.out.println("Grafo 2 generado...");

        createGraph("TestEmbebedFunctions.java", "grafoEmbebedFunctions");
        System.out.println("Grafo 3 generado...");

        createGraph("TestInlineFunctions.java", "grafoInlineVariables");
        System.out.println("Grafo 4 generado...");

        createGraph("TestExamplePaper.java", "grafoPaper");
        System.out.println("Grafo 5 generado...");

        createGraph("Test1.java", "grafoTest1");
        System.out.println("Grafo 6 generado...");

        createGraph("Test2.java", "grafoTest2");
        System.out.println("Grafo 7 generado...");

        createGraph("Test3.java", "grafoTest3");
        System.out.println("Grafo 8 generado...");

        createGraph("Test4.java", "grafoTest4");
        System.out.println("Grafo 9 generado...");

        createGraph("Test5.java", "grafoTest5");
        System.out.println("Grafo 10 generado...");

        createGraph("Test6.java", "grafoTest6");
        System.out.println("Grafo 11 generado...");*/

        /*createGraph("TestExamplePaperSimple.java", "grafoPaperSimple");
        System.out.println("Grafo 12 generado...");*/

        /*createGraph("TestExamplePaperSimpleContra1.java", "contra1");
        System.out.println("Grafo 13 generado...");*/

        /* createGraph("TestExamplePaperSimpleContra2.java", "contra2");
        System.out.println("Grafo 14 generado..."); */

        /*createGraph("TestExamplePaperSimpleContra3.java", "contra3");
        System.out.println("Grafo 15 generado...");*/

        /* createGraph("TestExamplePaperSimpleContra4.java", "contra4");
        System.out.println("Grafo 16 generado..."); */

        /* createGraph("TestExamplePaperSimpleContra5.java", "contra5");
        System.out.println("Grafo 17 generado..."); */

        /* createGraph("TestExamplePaperSimpleContra6.java", "contra6");
        System.out.println("Grafo 18 generado..."); */

        /* createGraph("TestExamplePaperSimpleContra7.java", "contra7");
        System.out.println("Grafo 19 generado..."); */

        createGraph("TestContraEjemploSimple.java", "contraSimple");
        System.out.println("Grafo 20 generado...");
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
