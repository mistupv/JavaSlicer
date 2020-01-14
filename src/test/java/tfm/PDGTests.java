package tfm;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tfm.exec.GraphLog;
import tfm.exec.PDGLog;
import tfm.graphs.CFG.ACFG;
import tfm.graphs.PDG;
import tfm.graphs.PDG.APDG;
import tfm.graphs.PDG.PPDG;
import tfm.nodes.GraphNode;
import tfm.slicing.GraphNodeCriterion;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Logger;
import tfm.visitors.cfg.CFGBuilder;
import tfm.visitors.pdg.ControlDependencyBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public class PDGTests {
    static {
        JavaParser.getStaticConfiguration().setAttributeComments(false);
    }

    private boolean error = false;

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.FileFinder#findAllMethodDeclarations")
    public void ppdgTest(File file, String methodName, MethodDeclaration root) throws IOException {
        runPdg(file, root, PDGLog.PPDG);
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.FileFinder#findAllMethodDeclarations")
    public void apdgTest(File file, String methodName, MethodDeclaration root) throws IOException {
        runPdg(file, root, PDGLog.APDG);
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.FileFinder#findAllMethodDeclarations")
    public void pdgTest(File file, String methodName, MethodDeclaration root) throws IOException {
        runPdg(file, root, PDGLog.PDG);
    }

    private void runPdg(File file, Node root, int type) throws IOException {
        GraphLog<?> graphLog = new PDGLog(type);
        graphLog.visit(root);
        graphLog.log();
        try {
            graphLog.generateImages(file.getPath());
        } catch (Exception e) {
            System.err.println("Could not generate PNG");
            System.err.println(e.getMessage());
        }
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.FileFinder#findAllMethodDeclarations")
    public void pdgCompare(File file, String methodName, MethodDeclaration root) throws IOException {
        ControlDependencyBuilder ctrlDepBuilder;

        if (containsUnsupportedStatements(root)) {
            System.err.println("Contains unsupported instructions");
        }

        // Create APDG
        ACFG acfg = new ACFG();
        root.accept(new CFGBuilder(acfg), null);
        APDG apdg = new APDG(acfg);
        ctrlDepBuilder = new ControlDependencyBuilder(apdg, acfg);
        ctrlDepBuilder.analyze();

        // Create PPDG
        PPDG ppdg = new PPDG(acfg);
        ctrlDepBuilder = new ControlDependencyBuilder(ppdg, acfg);
        ctrlDepBuilder.analyze();

        // Compare
        Logger.log("COMPARE APDG and PPDG");
        compareGraphs(apdg, ppdg);
        assert !error;
    }

    public static boolean containsUnsupportedStatements(Node node) {
        return node.findFirst(TryStmt.class).isPresent()
                || node.findFirst(ThrowStmt.class).isPresent();
    }


    /** Slices both graphs on every possible node and compares the result */
    public void compareGraphs(PDG pdg1, PDG pdg2) {
        for (GraphNode<?> node : pdg1.getNodes().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .collect(Collectors.toList())) {
            if (node.getAstNode() instanceof MethodDeclaration)
                continue;
            SlicingCriterion sc = new GraphNodeCriterion(node, "x");
            Set<Integer> slice1 = pdg1.slice(sc);
            Set<Integer> slice2 = pdg2.slice(sc);
            Logger.log("Slicing on " + node.getId());
            boolean ok = slice1.equals(slice2);
            if (!ok) {
                Logger.log("FAILED!");
                error = true;
            }
            printSlices(pdg1, slice1, slice2);
        }
    }

    @SafeVarargs
    public final void printSlices(PDG pdg, Set<Integer>... slices) {
        pdg.getNodes().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .forEach(n -> Logger.format("%3d: %s %s",
                        n.getId(),
                        Arrays.stream(slices)
                                .map(s -> s.contains(n.getId()) ? "x" : " ")
                                .reduce((a, b) -> a + " " + b),
                        n.getData()));
    }
}
