package es.upv.mist.slicing.benchmark;

import com.github.javaparser.ParseException;
import com.github.javaparser.Problem;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import es.upv.mist.slicing.arcs.pdg.StructuralArc;
import es.upv.mist.slicing.graphs.augmented.ASDG;
import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.graphs.exceptionsensitive.AllenSDG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.graphs.jsysdg.OriginalJSysDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.slicing.OriginalJSysDGSlicingAlgorithm;
import es.upv.mist.slicing.slicing.SlicingCriterion;
import es.upv.mist.slicing.utils.NodeHashSet;
import es.upv.mist.slicing.utils.StaticTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BenchSC {
    protected static final int BUILD_TIMES = 0, SLICE_TIMES = 1, SLICE_SIZES = 2, EXIT = 3;
    protected final String[] dirIncludeSet = System.getProperty("sInclude", "").split(":");
    protected String graphType;

    public void benchmark() {
        // Obtain parameters
        String baselineGraph = System.getProperty("sGraphBaseline");
        String benchGraph = System.getProperty("sGraphBench");
        int minIter = Integer.parseInt(System.getProperty("sMinIter", "100"));
        String outputPrefix = System.getProperty("sOutputPrefix", "result");

        // Files
        File buildBaseTime = new File(outputPrefix + "buildBaseTime.out");
        File buildBenchTime = new File(outputPrefix + "buildBenchTime.out");
        File nodeCount = new File(outputPrefix + "nodesBaseline.out");
        File sliceBaseTime = new File(outputPrefix + "sliceBaseTime.out");
        File sliceBenchTime = new File(outputPrefix + "sliceBenchTime.out");

        // Configure JavaParser
        StaticJavaParser.getConfiguration().setAttributeComments(false);
        StaticTypeSolver.addTypeSolverJRE();
        for (String directory : dirIncludeSet)
            StaticTypeSolver.addTypeSolver(new JavaParserTypeSolver(directory));

        while (true) {
            switch (selectOption()) {
                case BUILD_TIMES:
                    graphType = baselineGraph;
                    timedRun(this::buildGraph, minIter, buildBaseTime);
                    graphType = benchGraph;
                    timedRun(this::buildGraph, minIter, buildBenchTime);
                    break;
                case SLICE_SIZES:
                    try (PrintWriter pw = new PrintWriter(nodeCount)) {
                        graphType = "JSysDG";
                        SDG baseSDG = buildGraph();
                        pw.println("# File format: for each SDG type, the total number of nodes and then the number of ");
                        pw.println(baseSDG.vertexSet().size());
                        Collection<SlicingCriterion> baseCriteria = findSCs(baseSDG);
                        System.out.printf("There are %d return object SCs", findReturnObjectSCs(baseSDG).size());
                        System.out.printf("There are %d real nodes SCs", findRealSCs(baseSDG).size());
                        System.exit(0);
                        for (SlicingCriterion sc : baseCriteria) {
                            int baseNodes = new OriginalJSysDGSlicingAlgorithm((JSysDG) baseSDG).traverse(sc.findNode(baseSDG)).getGraphNodes().size();
                            int benchNodes = baseSDG.slice(sc).getGraphNodes().size();
                            pw.printf("\"%s\",%d,%d\n", sc, baseNodes, benchNodes);
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case SLICE_TIMES:
                    try {
                        graphType = baselineGraph;
                        SDG sdg1 = buildGraph();
                        try (PrintWriter pw = new PrintWriter(sliceBaseTime)) {
                            pw.println("# SC id, SC time sequence");
                            for (SlicingCriterion sc : findSCs(sdg1))
                                timedRun(() -> sdg1.slice(sc), minIter, pw);
                        }
                        graphType = benchGraph;
                        SDG sdg2 = buildGraph();
                        try (PrintWriter pw = new PrintWriter(sliceBenchTime)) {
                            pw.println("# SC id, SC time sequence");
                            for (SlicingCriterion sc : findSCs(sdg2))
                                timedRun(() -> sdg2.slice(sc), minIter, pw);
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case EXIT:
                    return;
            }
        }
    }

    protected int selectOption() {
        Scanner in = new Scanner(System.in);
        System.out.println("Select an option:");
        System.out.println("\t[0]: Time the building of the graphs");
        System.out.println("\t[1]: Time the slicing of the graphs");
        System.out.println("\t[2]: Number of nodes per slice");
        System.out.println("\t[3]: Exit");
        System.out.print("> ");
        return in.nextInt();
    }

    protected SDG buildGraph() {
        try {
            // Build the SDG
            Set<CompilationUnit> units = new NodeHashSet<>();
            List<Problem> problems = new LinkedList<>();
            for (File file : (Iterable<File>) findAllJavaFiles(dirIncludeSet)::iterator)
                parse(file, units, problems);
            if (!problems.isEmpty()) {
                for (Problem p : problems)
                    System.out.println(" * " + p.getVerboseMessage());
                throw new ParseException("Some problems were found while parsing files or folders");
            }

            SDG sdg = createGraph(graphType);
            sdg.build(new NodeList<>(units));
            return sdg;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void parse(File file, Set<CompilationUnit> units, List<Problem> problems) {
        try {
            units.add(StaticJavaParser.parse(file));
        } catch (FileNotFoundException e) {
            problems.add(new Problem(e.getLocalizedMessage(), null, e));
        }
    }

    protected Stream<File> findAllJavaFiles(String[] files) {
        Stream.Builder<File> builder = Stream.builder();
        for (String fileName : files) {
            File file = new File(fileName);
            if (file.isDirectory())
                findAllJavaFiles(file, builder);
            else
                builder.accept(file);
        }
        return builder.build();
    }

    protected void findAllJavaFiles(File directory, Stream.Builder<File> builder) {
        File[] files = directory.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            if (f.isDirectory())
                findAllJavaFiles(f, builder);
            else if (f.getName().endsWith(".java"))
                builder.accept(f);
        }
    }

    protected SDG createGraph(String graphName) {
        switch (graphName) {
            case "SDG": return new SDG();
            case "ASDG": return new ASDG();
            case "PSDG": return new PSDG();
            case "ESSDG": return new ESSDG();
            case "AllenSDG": return new AllenSDG();
            case "JSysDG": return new JSysDG();
            case "OriginalJSysDG": return new OriginalJSysDG();
            default:
                throw new IllegalArgumentException();
        }
    }

    protected long[] timedRun(Runnable runnable, int iterations) {
        long[] times = new long[iterations];
        long t1, t2;
        for (int i = -1; i < iterations; i++) {
            t1 = System.nanoTime();
            runnable.run();
            t2 = System.nanoTime();
            if (i >= 0)
                times[i] = t2 - t1; // Times stored in nanoseconds
        }
        return times;
    }

    protected void timedRun(Runnable runnable, int minIter, File file) {
        long[] data = timedRun(runnable, minIter);
        try (PrintWriter pw = new PrintWriter(file)) {
            for (long d : data)
                pw.println(d);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected void timedRun(Runnable runnable, int minIter, PrintWriter pw) {
        long[] data = timedRun(runnable, minIter);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0)
                builder.append(',');
            builder.append(data[i]);
        }
        pw.println(builder);
    }

    protected Collection<SlicingCriterion> findSCs(SDG sdg) {
        return findReturnObjectSCs(sdg);
    }

    protected Collection<SlicingCriterion> findRealSCs(SDG sdg) {
        return sdg.vertexSet().stream()
                .filter(Predicate.not(SyntheticNode.class::isInstance))
                .filter(Predicate.not(GraphNode::isImplicitInstruction))
                .sorted()
                .map(n -> (SlicingCriterion) graph -> Set.of(n))
                .collect(Collectors.toList());
    }

    protected Collection<SlicingCriterion> findReturnObjectSCs(SDG sdg) {
        return sdg.vertexSet().stream()
                .filter(gn -> gn.getAstNode() instanceof ReturnStmt)
                .flatMap(gn -> sdg.outgoingEdgesOf(gn).stream()
                        .filter(StructuralArc.class::isInstance)
                        .map(sdg::getEdgeTarget))
                .filter(gn -> sdg.outgoingEdgesOf(gn).stream().anyMatch(StructuralArc.class::isInstance))
                .map(n -> new SlicingCriterion() {
                    public Set<GraphNode<?>> findNode(SDG sdg) { return Set.of(n); }
                    public String toString() { return n.getLongLabel(); }
                })
                .collect(Collectors.toList());
    }

    public static void main(String... args) {
        new BenchSC().benchmark();
    }
}
