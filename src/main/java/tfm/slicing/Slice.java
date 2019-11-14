package tfm.slicing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import tfm.exec.PDGLog;
import tfm.graphs.PDGGraph;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.validation.PDGValidator;
import tfm.visitors.pdg.PDGBuilder;

import java.io.File;
import java.io.IOException;

public class Slice {

    public static final String PROGRAM_FOLDER = Utils.PROGRAMS_FOLDER + "pdg/";
    public static final String PROGRAM_NAME = "Example2";

    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parse(new File(PROGRAM_FOLDER + PROGRAM_NAME + ".java"));

        PDGGraph pdgGraph = new PDGGraph();

        compilationUnit.accept(new PDGBuilder(pdgGraph), pdgGraph.getRootNode());

        Logger.log("==================");
        Logger.log("= Starting slice =");
        Logger.log("==================");

        PDGGraph sliced = pdgGraph.slice(new LineNumberCriterion(18, "x"));

        PDGLog pdgLog = new PDGLog(sliced);
        pdgLog.log();
        pdgLog.generatePNGs(PROGRAM_NAME + "-sliced");
        pdgLog.openVisualRepresentation();

        PDGValidator.printPDGProgram("Slice" + PROGRAM_NAME, sliced);
    }
}
