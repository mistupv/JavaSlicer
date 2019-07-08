package tfm.slicing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import tfm.arcs.Arc;
import tfm.exec.GraphLog;
import tfm.exec.PDGLog;
import tfm.graphs.PDGGraph;
import tfm.nodes.Node;
import tfm.nodes.PDGNode;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.visitors.PDGCFGVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class Slice {

    public static final String PROGRAM = Utils.PROGRAMS_FOLDER + "pdg/Example2.java";

    public static void main(String[] args) throws IOException {
        CompilationUnit compilationUnit = JavaParser.parse(new File(PROGRAM));

        PDGGraph pdgGraph = new PDGGraph();

        compilationUnit.accept(new PDGCFGVisitor(pdgGraph), pdgGraph.getRootNode());

        Logger.log("==================");
        Logger.log("= Starting slice =");
        Logger.log("==================");

//        Logger.log(pdgGraph);

        Set<PDGNode> slice = pdgGraph.slice("x", 18);

        PDGGraph sliced = new PDGGraph();

        for (PDGNode node : slice) {
            sliced.addNode(node);
        }

        Set<Arc> arcs = pdgGraph.getArcs().stream()
                .filter(arc -> sliced.getNodes().contains(arc.getFrom()) || sliced.getNodes().contains(arc.getTo()))
                .collect(Collectors.toSet());
//                .forEach(sliced::addEdge);

        Logger.log("==== Arcs ====");
        sliced.getArcs().forEach(Logger::log);

//        Logger.log(sliced.getArcs().stream().map(Arc::toString).collect(Collectors.joining(System.lineSeparator())));

        PDGLog pdgLog = new PDGLog(pdgGraph);
        pdgLog.log();

        Logger.log(slice.stream()
                .sorted(Comparator.comparingInt(Node::getId))
                .map(Node::toString)
                .collect(Collectors.joining(System.lineSeparator())));
    }
}
