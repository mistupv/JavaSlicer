package tfm.graphs.pdg;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tfm.graphs.augmented.ACFG;
import tfm.graphs.augmented.APDG;
import tfm.graphs.augmented.PPDG;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.nodes.TypeNodeFactory;
import tfm.nodes.type.NodeType;
import tfm.slicing.GraphNodeCriterion;
import tfm.slicing.Slice;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PDGTests {
    static {
        JavaParser.getStaticConfiguration().setAttributeComments(false);
    }

    private boolean error = false;

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.utils.FileFinder#findAllMethodDeclarations")
    public void ppdgTest(File file, String methodName, MethodDeclaration root) throws IOException {
        runPdg(file, methodName, root, new PPDG());
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.utils.FileFinder#findAllMethodDeclarations")
    public void apdgTest(File file, String methodName, MethodDeclaration root) throws IOException {
        runPdg(file, methodName, root, new APDG());
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.utils.FileFinder#findAllMethodDeclarations")
    public void pdgTest(File file, String methodName, MethodDeclaration root) throws IOException {
        runPdg(file, methodName, root, new PDG());
    }

    private void runPdg(File file, String methodName, MethodDeclaration root, PDG pdg) throws IOException {
        pdg.build(root);
//        GraphLog<?> graphLog = new PDGLog(pdg);
//        graphLog.log();
//        try {
//            graphLog.generateImages(file.getPath() + "-" + methodName);
//        } catch (Exception e) {
//            System.err.println("Could not generate PNG");
//            System.err.println(e.getMessage());
//        }
    }

    @ParameterizedTest(name = "[{index}] {0} ({1})")
    @MethodSource("tfm.utils.FileFinder#findAllMethodDeclarations")
    public void pdgCompare(File file, String methodName, MethodDeclaration root) {
        ControlDependencyBuilder ctrlDepBuilder;

        if (containsUnsupportedStatements(root)) {
            System.err.println("Contains unsupported instructions");
        }

        // Create PDG
        CFG cfg = new CFG();
        cfg.build(root);
        PDG pdg = new PDG(cfg);
        pdg.buildRootNode("ENTER " + methodName, root, TypeNodeFactory.fromType(NodeType.METHOD_ENTER));
        ctrlDepBuilder = new ControlDependencyBuilder(cfg, pdg);
        ctrlDepBuilder.build();

        // Create APDG
        ACFG acfg = new ACFG();
        acfg.build(root);
        APDG apdg = new APDG(acfg);
        apdg.buildRootNode("ENTER " + methodName, root, TypeNodeFactory.fromType(NodeType.METHOD_ENTER));
        ctrlDepBuilder = new ControlDependencyBuilder(acfg, apdg);
        ctrlDepBuilder.build();

        // Create PPDG
        PPDG ppdg = new PPDG(acfg);
        ppdg.buildRootNode("ENTER " + methodName, root, TypeNodeFactory.fromType(NodeType.METHOD_ENTER));
        ctrlDepBuilder = new ControlDependencyBuilder(acfg, ppdg);
        ctrlDepBuilder.build();

        // Print graphs (commented to decrease the test's time)
        String filePathNoExt = file.getPath().substring(0, file.getPath().lastIndexOf('.'));
//        String name = filePathNoExt + "/" + methodName;
//        new PDGLog(pdg).generateImages(name);
//        new PDGLog(apdg).generateImages(name);
//        new PDGLog(ppdg).generateImages(name);

        // Compare
        List<MethodDeclaration> slicedMethods = compareGraphs(pdg, apdg, ppdg);

        // Write sliced methods to a java file.
        ClassOrInterfaceDeclaration clazz = new ClassOrInterfaceDeclaration();
        slicedMethods.forEach(clazz::addMember);
        clazz.setName(methodName);
        clazz.setModifier(Modifier.Keyword.PUBLIC, true);
        try (PrintWriter pw = new PrintWriter(new File("./out/" + filePathNoExt + "/" + methodName + ".java"))) {
            pw.println(clazz);
        } catch (Exception e) {
            Logger.log("Error! Could not write classes to file");
        }

        assert !error;
    }

    public static boolean containsUnsupportedStatements(Node node) {
        return node.findFirst(TryStmt.class).isPresent()
                || node.findFirst(ThrowStmt.class).isPresent();
    }


    /** Slices both graphs on every possible node and compares the result */
    public List<MethodDeclaration> compareGraphs(PDG... pdgs) {
        List<MethodDeclaration> slicedMethods = new LinkedList<>();
        assert pdgs.length > 0;
        for (GraphNode<?> node : pdgs[0].vertexSet().stream()
                .sorted(Comparator.comparingLong(GraphNode::getId))
                .collect(Collectors.toList())) {
            // Skip start of graph
            if (node.getAstNode() instanceof MethodDeclaration)
                continue;

            // Perform slices
            SlicingCriterion sc = new GraphNodeCriterion(node, "x");
//            Slice[] slices = Arrays.stream(pdgs).map(p -> p.slice(sc)).toArray(Slice[]::new);

            // Compare slices
            boolean ok = true;
//            Slice referenceSlice = slices[0];
//            for (Slice slice : slices) {
//                ok = referenceSlice.equals(slice);
//                error |= !ok;
//                if (!ok) break;
//            }

            // Display slice
            Logger.log("Slicing on " + node.getId());
            if (!ok)
                Logger.log("FAILED!");
//            printSlices(pdgs[0], slices);

            // Save slices as MethodDeclaration
            int i = 0;
//            for (Slice s : slices) {
//                i++;
//                try {
//                    MethodDeclaration m = ((MethodDeclaration) s.getAst());
//                    m.setName(m.getName() + "_slice" + node.getId() + "_pdg" + i);
//                    slicedMethods.add(m);
//                } catch (RuntimeException e) {
//                    Logger.log("Error: " + e.getMessage());
//                }
//            }
        }
        return slicedMethods;
    }

    public final void printSlices(PDG pdg, Slice... slices) {
        pdg.vertexSet().stream()
                .sorted(Comparator.comparingLong(GraphNode::getId))
                .forEach(n -> Logger.format("%3d: %s %s",
                        n.getId(),
                        Arrays.stream(slices)
                                .map(s -> s.contains(n) ? "x" : " ")
                                .reduce((a, b) -> a + " " + b).orElse("--error--"),
                        n.getInstruction()));
    }
}
