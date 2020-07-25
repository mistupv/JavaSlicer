package tfm.cli;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.cli.*;
import tfm.graphs.augmented.ASDG;
import tfm.graphs.augmented.PSDG;
import tfm.graphs.cfg.CFG;
import tfm.graphs.exceptionsensitive.ESSDG;
import tfm.graphs.sdg.SDG;
import tfm.slicing.NodeIdSlicingCriterion;
import tfm.slicing.Slice;
import tfm.slicing.SlicingCriterion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class PHPSlice {
    protected static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption(Option
                .builder("f").longOpt("file")
                .hasArg().argName("CriterionFile.java").type(File.class)
                .required()
                .desc("The file that contains the slicing criterion.")
                .build());
        OPTIONS.addOption(Option
                .builder("i").longOpt("node-id")
                .hasArg().argName("node_id")
                .required()
                .desc("The slicing criterion, in the form of a node id (a positive integer).")
                .build());
        OPTIONS.addOption(Option
                .builder("o").longOpt("output-dir")
                .hasArg().argName("output_file")
                .required()
                .desc("The folder where the slice and the graphs should be placed")
                .build());
        OPTIONS.addOption(Option
                .builder("t").longOpt("type")
                .hasArg().argName("graph_type")
                .desc("The type of graph to be built. Available options are SDG, ASDG, PSDG, ESSDG.")
                .build());
        OPTIONS.addOption("es", "exception-sensitive", false, "Enable exception-sensitive analysis");
        OPTIONS.addOption(Option
                .builder("h").longOpt("help")
                .desc("Shows this text")
                .build());
    }

    private final File outputDir;
    private File scFile;
    private int scId;
    private final CommandLine cliOpts;

    public PHPSlice(String... cliArgs) throws ParseException {
        cliOpts = new DefaultParser().parse(OPTIONS, cliArgs);
        if (cliOpts.hasOption('h'))
            throw new ParseException(OPTIONS.toString());
        setScId(Integer.parseInt(cliOpts.getOptionValue("i")));
        setScFile(cliOpts.getOptionValue("f"));
        outputDir = new File(cliOpts.getOptionValue("o"));
        if (!outputDir.isDirectory())
            throw new ParseException("The output directory is not a directory or not readable by us!");
    }

    private void setScFile(String fileName) throws ParseException {
        File file = new File(fileName);
        if (!(file.exists() && file.isFile()))
            throw new ParseException("Slicing criterion file is not an existing file.");
        scFile = file;
    }

    private void setScId(int line) throws ParseException {
        if (line < 0)
            throw new ParseException("The line of the slicing criterion must be strictly greater than zero.");
        scId = line;
    }

    public void slice() throws ParseException, IOException {
        // Configure JavaParser
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver(true));
        JavaParser.getStaticConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
        JavaParser.getStaticConfiguration().setAttributeComments(false);

        // Build the SDG
        NodeList<CompilationUnit> units = new NodeList<>();
        try {
            units.add(JavaParser.parse(scFile));
        } catch (FileNotFoundException e) {
            throw new ParseException(e.getMessage());
        }

        SDG sdg;
        switch (cliOpts.getOptionValue("type")) {
            case "SDG":   sdg = new SDG();   break;
            case "ASDG":  sdg = new ASDG();  break;
            case "PSDG":  sdg = new PSDG();  break;
            case "ESSDG": sdg = new ESSDG(); break;
            default:
                throw new IllegalArgumentException("Unknown type of graph. Available graphs are SDG, ASDG, PSDG, ESSDG");
        }
        sdg.build(units);

        SlicingCriterion sc = new NodeIdSlicingCriterion(0, "");
        Slice slice = new Slice();
        if (scId != 0) {
            // Slice the SDG
            sc = new NodeIdSlicingCriterion(scId, "");
            slice = sdg.slice(sc);

            // Convert the slice to code and output the result to `outputDir`
            for (CompilationUnit cu : slice.toAst()) {
                if (cu.getStorage().isEmpty())
                    throw new IllegalStateException("A synthetic CompilationUnit was discovered, with no file associated to it.");
                File javaFile = new File(outputDir, cu.getStorage().get().getFileName());
                try (PrintWriter pw = new PrintWriter(javaFile)) {
                    pw.print(new BlockComment(getDisclaimer(cu.getStorage().get())));
                    pw.print(cu);
                } catch (FileNotFoundException e) {
                    System.err.println("Could not write file " + javaFile);
                }
            }
        }

        File imageDir = new File(outputDir, "images");
        imageDir.mkdir();
        // Output the sliced graph to the output directory
        SDGLog sdgLog = new SlicedSDGLog(sdg, slice, sc);
        sdgLog.setDirectory(outputDir);
        sdgLog.generateImages("graph", "svg");
        for (CFG cfg : sdg.getCFGs()) {
            CFGLog log = new CFGLog(cfg);
            log.setDirectory(imageDir);
            log.generateImages("root" + cfg.getRootNode().getId(), "svg");
        }
    }

    protected String getDisclaimer(CompilationUnit.Storage s) {
        return String.format("\n\tThis file was automatically generated as part of a slice with criterion" +
                        "\n\tnode id: %d\n\tOriginal file: %s\n", scId, s.getPath());
    }

    public static void main(String... args) {
        try {
            new PHPSlice(args).slice();
        } catch (Exception e) {
            System.err.println("Error!\n" + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

}
