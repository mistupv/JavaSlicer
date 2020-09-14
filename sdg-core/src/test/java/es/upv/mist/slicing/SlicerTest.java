package es.upv.mist.slicing;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.slicing.FileLineSlicingCriterion;
import es.upv.mist.slicing.slicing.Slice;
import es.upv.mist.slicing.slicing.SlicingCriterion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Scanner;

public class SlicerTest {
    static {
        JavaParser.getStaticConfiguration().setAttributeComments(false);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver(true));
        JavaParser.getStaticConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        JavaParser.getStaticConfiguration().setAttributeComments(false);
    }

    private static final String TEST_FILES = "./sdg-core/src/test/res/dinsa-tests";
    private static final String DOT_JAVA = ".java";

    public static Collection<Arguments> findFiles(File directory, String suffix) throws FileNotFoundException {
        Collection<Arguments> res = new LinkedList<>();
        File[] files = directory.listFiles();
        if (files == null) return Collections.emptyList();
        for (File f : files) {
            if (f.getName().endsWith(suffix))
                res.add(Arguments.of(f, getSliceFile(f), getCriterionLine(f)));
            if (f.isDirectory())
                res.addAll(findFiles(f, suffix));
        }
        return res;
    }

    public static Arguments[] findAllFiles() throws FileNotFoundException {
        Collection<Arguments> args = findFiles(new File(TEST_FILES), DOT_JAVA);
        return args.toArray(Arguments[]::new);
    }

    private static File getSliceFile(File file) {
        return new File(file.getParent(), file.getName() + ".sdg.sliced");
    }

    private static int getCriterionLine(File file) throws FileNotFoundException {
        return new Scanner(new File(file.getParent(), file.getName() + ".sdg.criterion")).nextInt();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("findAllFiles")
    public void sdgCompare(File source, File target, int criterionLine) throws FileNotFoundException {
        // Build the SDG
        SDG sdg = new ESSDG();
        sdg.build(new NodeList<>(JavaParser.parse(source)));
        SlicingCriterion sc = new FileLineSlicingCriterion(source, criterionLine);
        Slice slice = sdg.slice(sc);

        // Convert the slice to code and output the result to `outputDir`
        NodeList<CompilationUnit> slicedUnits = slice.toAst();
        assert slicedUnits.size() == 1;
        if (!target.exists()) {
            try (PrintWriter pw = new PrintWriter(target)) {
                pw.print(slicedUnits.get(0).toString());
            }
            return;
        }
        String targetSlice;
        {
            StringBuilder builder = new StringBuilder();
            Scanner in = new Scanner(target);
            while (in.hasNextLine())
                builder.append(in.nextLine()).append('\n');
            targetSlice = builder.toString();
        }
        String ourSlice = slicedUnits.get(0).toString();
        boolean equal = targetSlice.equals(ourSlice);
        assert equal;
    }
}
