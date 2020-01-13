package tfm.slicing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import tfm.graphs.PDG;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.visitors.pdg.PDGBuilder;

import java.io.File;
import java.io.IOException;

public class Slice {

    public static final String PROGRAM_FOLDER = Utils.PROGRAMS_FOLDER + "pdg/";
    public static final String PROGRAM_NAME = "Example2";

    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parse(new File(PROGRAM_FOLDER + PROGRAM_NAME + ".java"));

        PDG pdg = new PDG();

        compilationUnit.accept(new PDGBuilder(pdg), pdg.getRootNode());

        Logger.log("==================");
        Logger.log("= Starting slice =");
        Logger.log("==================");

//        PDGGraph sliced = pdgGraph.slice(new LineNumberCriterion(18, "x"));
//
//        PDGLog pdgLog = new PDGLog(sliced);
//        pdgLog.log();
//        pdgLog.generateImages(PROGRAM_NAME + "-sliced");
//        pdgLog.openVisualRepresentation();
//
//        PDGValidator.printPDGProgram("Slice" + PROGRAM_NAME, sliced);
    }
}
