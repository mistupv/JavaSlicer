package es.upv.mist.slicing;

import com.github.javaparser.StaticJavaParser;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SlicerTest {
    static {
        StaticJavaParser.getConfiguration().setAttributeComments(false);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver(true));
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        StaticJavaParser.getConfiguration().setAttributeComments(false);
    }

    private static final String TEST_PKG = "regression";
    private static final String DOT_JAVA = ".java";
    private static final String SDG_CRITERION = ".sdg.criterion";
    private static final String SDG_SLICE = ".sdg.sliced";

    public static void findFiles(File directory, String suffix, Consumer<File> consumer) {
        File[] files = directory.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            if (f.isDirectory())
                findFiles(f, suffix, consumer);
            else if (f.getName().endsWith(suffix))
                consumer.accept(f);
        }
    }

    public static Arguments[] findAllFiles() {
        Collection<Arguments> args = new LinkedList<>();
        File testFolder = new File(Thread.currentThread().getContextClassLoader().getResource(TEST_PKG).getPath());
        findFiles(testFolder, DOT_JAVA, f -> createArgumentForTest(f).ifPresent(args::add));
        return args.toArray(Arguments[]::new);
    }

    private static Optional<Arguments> createArgumentForTest(File javaFile) {
        File slice = new File(javaFile.getParent(), javaFile.getName() + SDG_SLICE);
        Optional<SlicingCriterion> criterion = findSDGCriterion(javaFile);
        if (!slice.isFile() || !slice.canRead() || criterion.isEmpty())
            return Optional.empty();
        return Optional.of(Arguments.of(javaFile, slice, criterion.get()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("findAllFiles")
    public void slicerRegressionTest(File source, File target, SlicingCriterion sc) throws FileNotFoundException {
        if (!target.exists())
            return;
        Slice slice = slice(source ,sc);
        boolean equal = slicesMatch(slice, target);
        assert equal: "The slice for " + source.toString() + " has change, please fix the error or update the reference slice.";
    }

    @Test
    public void generateDefaultSlices() {
        File testFolder = new File("./sdg-core/src/test/res/", TEST_PKG);
        findFiles(testFolder, DOT_JAVA, SlicerTest::createAndSaveSlice);
    }

    private static Optional<SlicingCriterion> findSDGCriterion(File javaFile) {
        File criterionFile = new File(javaFile.getParentFile(), javaFile.getName() + SDG_CRITERION);
        try (Scanner in = new Scanner(criterionFile)) {
            return Optional.of(new FileLineSlicingCriterion(javaFile, in.nextInt()));
        } catch (FileNotFoundException | NoSuchElementException e) {
            return Optional.empty();
        }
    }

    private static void createAndSaveSlice(File javaFile) {
        try {
            File sliceFile = new File(javaFile.getParent(), javaFile.getName() + SDG_SLICE);
            Optional<SlicingCriterion> sc = findSDGCriterion(javaFile);
            if (sc.isEmpty() || sliceFile.exists())
                return;
            Slice slice = slice(javaFile, sc.get());
            var cus = slice.toAst();
            assert cus.size() == 1;
            try (PrintWriter pw = new PrintWriter(sliceFile)) {
                pw.write(cus.getFirst().get().toString());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not save slice due to missing file or permissions");
        } catch (Exception e) {
            System.err.println("Error generating slice for " + javaFile.toString());
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean slicesMatch(Slice slice, File referenceSlice) {
        NodeList<CompilationUnit> cus = slice.toAst();
        assert cus.size() == 1;
        return Objects.equals(cus.getFirst().get().toString(), readFile(referenceSlice));
    }

    private static String readFile(File file, Supplier<String> separator) {
        try (Scanner in = new Scanner(file)) {
            StringBuilder builder = new StringBuilder();
            while (in.hasNextLine())
                builder.append(in.nextLine()).append(separator.get());
            return builder.toString();
        } catch (FileNotFoundException e) {
            return "";
        }
    }

    private static String readFile(File file) {
        return readFile(file, () -> "\n");
    }

    private static Slice slice(File javaFile, SlicingCriterion sc) throws FileNotFoundException {
        SDG sdg = new ESSDG();
        sdg.build(new NodeList<>(StaticJavaParser.parse(javaFile)));
        return sdg.slice(sc);
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        File testFolder = new File("./sdg-core/src/test/res/", TEST_PKG);
        findFiles(testFolder, DOT_JAVA, file -> {
           File sliceFile = new File(file.getParent(), file.getName() + SDG_CRITERION);
           if (!sliceFile.exists()) {
               int[] counter = new int[] { 1 };
               System.out.printf("%3d", counter[0]++);
               System.out.print(readFile(file, () -> String.format("\n%3d", counter[0]++)));
               System.out.printf("No criterion found for this program (%s), please input one (-1 to skip)\nCriterion: ", file);
               int line = in.nextInt();
               if (line == -1)
                   return;
               while (line <= 0 || line >= counter[0] - 2) {
                   System.out.printf("Your input is out-of-bounds, please try again [1-%d]: ", counter[0] - 2);
                   line = in.nextInt();
               }
               System.out.printf("Saving line %d as slicing criterion for %s... ", line, file);
               try (PrintWriter pw = new PrintWriter(sliceFile)) {
                   pw.write(line + "");
                   System.out.println("DONE");
               } catch (FileNotFoundException e) {
                   System.out.println("ERROR");
               }
           }
        });
    }
}
