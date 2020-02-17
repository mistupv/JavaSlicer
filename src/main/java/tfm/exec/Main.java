package tfm.exec;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.graphs.cfg.CFG;
import tfm.graphs.Graph;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
import tfm.utils.Logger;
import tfm.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class Main {

    public static final String PROGRAM = Utils.PROGRAMS_FOLDER + "sdg/Example1.java";
    public static final String GRAPH = GraphLog.SDG;
    public static final String METHOD = "main";

    public static void main(String[] args) throws IOException {
        JavaParser.getStaticConfiguration().setAttributeComments(false);

        // File
        File file = new File(PROGRAM);
        Node root = JavaParser.parse(file);

        if (!METHOD.isEmpty()) {
            Optional<MethodDeclaration> methodDeclarationOptional = root.findFirst(MethodDeclaration.class,
                    methodDeclaration -> Objects.equals(methodDeclaration.getNameAsString(), METHOD));

            if (!methodDeclarationOptional.isPresent()) {
                Logger.format("Method '%s' not found in '%s'. Exiting...", METHOD, PROGRAM);
                return;
            }

            root = methodDeclarationOptional.get();
        }

        // GraphLog
        long t0 = System.nanoTime();
        Graph graph = getBuiltGraph(args.length == 1 ? args[0] : GRAPH, (MethodDeclaration) root);
        long tf = System.nanoTime();
        long tt = tf - t0;

        GraphLog<?> graphLog = getGraphLog(graph);
        graphLog.log();
        graphLog.openVisualRepresentation();

        Logger.log();
        Logger.format("Graph generated in %.2f ms", tt / 10e6);
    }

    private static Graph getBuiltGraph(String graph, MethodDeclaration method) {
        switch (graph) {
            case GraphLog.CFG:
                CFG cfg = new CFG();
                cfg.build(method);
                return cfg;
            case GraphLog.PDG:
                PDG pdg = new PDG();
                pdg.build(method);
                return pdg;
            case GraphLog.SDG:
                SDG sdg = new SDG();
                sdg.build(new NodeList<>(method.findCompilationUnit().get()));
                return sdg;
            default:
                Logger.log("Unkown graph type");
                System.exit(1);
                return null;
        }
    }

    private static GraphLog<?> getGraphLog(Graph graph) {
        if (graph instanceof CFG)
            return new CFGLog((CFG) graph);
        else if (graph instanceof PDG)
            return new PDGLog((PDG) graph);
        else if (graph instanceof SDG)
            return new SDGLog((SDG) graph);
        Logger.log("Unknown graph type");
        System.exit(1);
        return null;
    }
}
