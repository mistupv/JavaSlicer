package es.upv.mist.slicing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.slicing.FileLineSlicingCriterion;
import es.upv.mist.slicing.slicing.Slice;
import es.upv.mist.slicing.slicing.SlicingCriterion;
import es.upv.mist.slicing.utils.StaticTypeSolver;
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
        StaticTypeSolver.addTypeSolverJRE();
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
        findFiles(testFolder, DOT_JAVA, f -> args.addAll(createArgumentForTest(f)));
        return args.toArray(Arguments[]::new);
    }

    private static Collection<Arguments> createArgumentForTest(File javaFile) {
        List<SlicingCriterion> criteria = findSDGCriterion(javaFile);
        Collection<Arguments> args = new LinkedList<>();
        for (int i = 0; i < criteria.size(); i++) {
            File slice = generateSliceFile(javaFile, i);
            if (slice.isFile() && slice.canRead())
                args.add(Arguments.of(javaFile, slice, criteria.get(i)));
        }
        return args;
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("findAllFiles")
    public void slicerRegressionTest(File source, File target, SlicingCriterion sc) throws FileNotFoundException {
        assert target.exists(): "The reference slice does not exist!";
        Slice slice = slice(source ,sc);
        boolean equal = slicesMatch(slice, target);
        assert equal: "The slice for " + source.toString() + " has changed, please fix the error or update the reference slice.";
    }

    @Test
    public void generateDefaultSlices() {
        File testFolder = new File("./sdg-core/src/test/res/", TEST_PKG);
        findFiles(testFolder, DOT_JAVA, SlicerTest::createAndSaveSlice);
    }

    private static List<SlicingCriterion> findSDGCriterion(File javaFile) {
        File criterionFile = new File(javaFile.getParentFile(), javaFile.getName() + SDG_CRITERION);
        try (Scanner in = new Scanner(criterionFile)) {
            List<SlicingCriterion> criteria = new LinkedList<>();
            while (in.hasNextInt()) {
                int line = in.nextInt();
                String var = null;
                if (in.hasNext())
                    var = in.next();
                criteria.add(new FileLineSlicingCriterion(javaFile, line, var));
            }
            return criteria;
        } catch (FileNotFoundException | NoSuchElementException e) {
            return new LinkedList<>();
        }
    }

    private static void createAndSaveSlice(File javaFile) {
        try {
            List<SlicingCriterion> criteria = findSDGCriterion(javaFile);
            for (int i = 0; i < criteria.size(); i++) {
                File sliceFile = generateSliceFile(javaFile, i);
                if (sliceFile.exists())
                    continue;
                Slice slice = slice(javaFile, criteria.get(i));
                var cus = slice.toAst();
                assert cus.size() == 1;
                try (PrintWriter pw = new PrintWriter(sliceFile)) {
                    pw.write(cus.getFirst().get().toString());
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Could not save slice due to missing file or permissions");
        } catch (Exception e) {
            System.err.println("Error generating slice for " + javaFile.toString());
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static File generateSliceFile(File javaFile, int scIndex) {
        return new File(javaFile.getParent(), javaFile.getName() + SDG_SLICE + (scIndex > 0 ? "." + scIndex : ""));
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
        SDG sdg = new JSysDG();
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
