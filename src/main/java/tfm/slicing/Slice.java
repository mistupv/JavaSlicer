package tfm.slicing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import tfm.exec.PDGLog;
import tfm.graphs.PDGGraph;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.validation.PDGValidator;
import tfm.visitors.PDGCFGVisitor;

import java.io.File;
import java.io.IOException;

public class Slice {

    public static final String PROGRAM_FOLDER = Utils.PROGRAMS_FOLDER + "pdg/";
    public static final String PROGRAM_NAME = "Example3";

    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parse(new File(PROGRAM_FOLDER + PROGRAM_NAME + ".java"));

        PDGGraph pdgGraph = new PDGGraph();

        compilationUnit.accept(new PDGCFGVisitor(pdgGraph), pdgGraph.getRootNode());

        Logger.log("==================");
        Logger.log("= Starting slice =");
        Logger.log("==================");

        PDGGraph sliced = pdgGraph.slice(new LineNumberCriterion(21, "sum"));

//        for (PDGNode node : slice) {
//            sliced.addNode(new PDGNode(node.getId(), node.getData(), node.getAstNode()));
//        }
//
//        for (Arc arc : pdgGraph.getArcs()) {
//
//            Optional<PDGNode> fromOptional = sliced.findNodeById(arc.getFromNode().getId());
//            Optional<PDGNode> toOptional = sliced.findNodeById(arc.getToNode().getId());
//
//            if (fromOptional.isPresent() && toOptional.isPresent()) {
//                PDGNode from = fromOptional.get();
//                PDGNode to = toOptional.get();
//
//                if (arc.isControlDependencyArrow()) {
//                    sliced.addControlDependencyArc(from, to);
//                } else {
//                    DataDependencyArc dataDependencyArc = (DataDependencyArc) arc;
//                    sliced.addDataDependencyArc(from, to, dataDependencyArc.getData().getVariables().get(0));
//                }
//            }

//            Logger.log(arc);
//            Logger.log("Must add? " + (sliceIds.contains(arc.getFromNode().getId()) && sliceIds.contains(arc.getToNode().getId())));
//
//            if (sliceIds.contains(arc.getFromNode().getId()) && sliceIds.contains(arc.getToNode().getId())) {
//                Logger.format("Added? %s", sliced.addEdge(arc));
//            }
//        }

//        Set<Arc> arcs = pdgGraph.getArcs().stream()
//                .filter(arc -> sliced.getNodes().contains(arc.getFrom()) || sliced.getNodes().contains(arc.getTo()))
//                .collect(Collectors.toSet());
////                .forEach(sliced::addEdge);
//
//        arcs.forEach(sliced::addEdge);

//        Logger.log("==== Arcs ====");
//        sliced.getArcs().forEach(Logger::log);

//        Logger.log(sliced.getArcs().stream().map(Arc::toString).collect(Collectors.joining(System.lineSeparator())));

        PDGLog pdgLog = new PDGLog(sliced);
        pdgLog.log();
        pdgLog.generatePNGs(PROGRAM_NAME + "-sliced");
        pdgLog.openVisualRepresentation();

        PDGValidator.printPDGProgram("Slice" + PROGRAM_NAME, sliced);
        PDGValidator.printPDGProgram("Generated" + PROGRAM_NAME, pdgGraph);
    }
}
