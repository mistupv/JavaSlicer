package tfm.exec;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.utils.Logger;
import tfm.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class Main {

    public static final String PROGRAM = Utils.PROGRAMS_FOLDER + "cfg/Eval_4.java";
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
        GraphLog<?> graphLog = getGraphLog(args.length == 1 ? args[0] : GRAPH);

        long t0 = System.nanoTime();
        graphLog.visit(root);
        long tf = System.nanoTime();

        long tt = tf - t0;

        graphLog.log();
        graphLog.openVisualRepresentation();

        Logger.log();
        Logger.format("Graph generated in %.2f ms", tt / 10e6);
    }

    private static GraphLog<?> getGraphLog(String graph) throws IOException {
        GraphLog<?> graphLog = null;

        switch (graph) {
            case GraphLog.CFG:
                graphLog = new CFGLog();
                break;
            case GraphLog.PDG:
                graphLog = new PDGLog();
                break;
            case GraphLog.SDG:
                graphLog = new SDGLog();
                break;
            default:
                Logger.log("Unkown graph type");
                System.exit(1);
        }

        return graphLog;
    }
}
