package es.upv.mist.slicing.cli;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import es.upv.mist.slicing.graphs.augmented.ASDG;
import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.slicing.FileLineSlicingCriterion;
import es.upv.mist.slicing.slicing.Slice;
import es.upv.mist.slicing.slicing.SlicingCriterion;
import es.upv.mist.slicing.utils.NodeHashSet;
import es.upv.mist.slicing.utils.StaticTypeSolver;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Slicer {
    protected static final String HELP_HEADER = "Java SDG Slicer: extract a slice from a Java program. At least" +
            " the \"-c\" flag must be used to specify the slicing criterion.";

    protected static final Pattern SC_PATTERN;
    protected static final File DEFAULT_OUTPUT_DIR = new File("./slice/");
    protected static final Options OPTIONS = new Options();

    static {
        String fileRe = "(?<file>[^#]+\\.java)";
        String lineRe = "(?<line>[1-9]\\d*)";
        String varsRe = "(?<var>[a-zA-Z_]\\w*(?:,[a-zA-Z_]\\w*)*)";
        SC_PATTERN = Pattern.compile(fileRe + "#" + lineRe + "(?::" + varsRe + ")?");
    }

    static {
        OPTIONS.addOption(Option
                .builder("f").longOpt("file")
                .hasArg().argName("CriterionFile.java").type(File.class)
                .desc("The file that contains the slicing criterion.")
                .build());
        OPTIONS.addOption(Option
                .builder("l").longOpt("line")
                .hasArg().argName("line-number").type(Number.class)
                .desc("The line that contains the statement of the slicing criterion.")
                .build());
        OPTIONS.addOption(Option
                .builder("v").longOpt("var")
                .hasArgs().argName("variable-name")
                .desc("The name of the variable of the slicing criterion. Not setting this option is" +
                        " equivalent to selecting all the code in the given line number.")
                .build());
        OPTIONS.addOption(Option
                .builder("c").longOpt("criterion")
                .hasArg().argName("file#line[:var]")
                .desc("The slicing criterion, in the format \"file#line:var\". The variable is optional."+
                        " This option may be replaced by \"-f\", \"-l\" and \"-v\"." +
                        " If this argument is set, it will override the individual ones.")
                .build());
        OPTIONS.addOption(Option
                .builder("i").longOpt("include")
                .hasArgs().argName("directory[,directory,...]").valueSeparator(',')
                .desc("Includes the directories listed in the search for methods called from the slicing criterion " +
                        "(directly or transitively). Methods that are not included here or part of the JRE, including" +
                        " third party libraries will not be analyzed, resulting in less precise slicing.")
                .build());
        OPTIONS.addOption(Option
                .builder("o").longOpt("output")
                .hasArg().argName("output-dir")
                .desc("The directory where the sliced source code should be placed. By default, it is placed at " +
                        DEFAULT_OUTPUT_DIR)
                .build());
        OPTIONS.addOption(Option
                .builder("t").longOpt("type")
                .hasArg().argName("graph-type")
                .desc("The type of graph to be built. Available options are SDG, ASDG, PSDG, ESSDG.")
                .build());
        OPTIONS.addOption(Option
                .builder("h").longOpt("help")
                .desc("Shows this text")
                .build());
    }

    private final Set<File> dirIncludeSet = new HashSet<>();
    private File outputDir = DEFAULT_OUTPUT_DIR;
    private File scFile;
    private int scLine;
    private String scVar;
    private final CommandLine cliOpts;

    public Slicer(String... cliArgs) throws ParseException {
        cliOpts = new DefaultParser().parse(OPTIONS, cliArgs);
        if (cliOpts.hasOption('h'))
            printHelp();
        if (cliOpts.hasOption('c')) {
            Matcher matcher = SC_PATTERN.matcher(cliOpts.getOptionValue("criterion"));
            if (!matcher.matches())
                throw new ParseException("Invalid format for slicing criterion, see --help for more details");
            setScFile(matcher.group("file"));
            setScLine(Integer.parseInt(matcher.group("line")));
            String var = matcher.group("var");
            if (var != null)
                setScVar(var);
        } else if (cliOpts.hasOption('f') && cliOpts.hasOption('l')) {
            setScFile(cliOpts.getOptionValue('f'));
            setScLine(((Number) cliOpts.getParsedOptionValue("l")).intValue());
            if (cliOpts.hasOption('v'))
                setScVar(cliOpts.getOptionValue('v'));
        } else {
            throw new ParseException("Slicing criterion not specified: either use \"-c\" or \"-f\" and \"-l\".");
        }

        if (cliOpts.hasOption('o'))
            outputDir = new File(cliOpts.getOptionValue("o"));

        if (cliOpts.hasOption('i')) {
            for (String str : cliOpts.getOptionValues('i')) {
                File dir = new File(str);
                if (!dir.isDirectory())
                    throw new ParseException("One of the include directories is not a directory or isn't accesible: " + str);
                dirIncludeSet.add(dir);
            }
        }
    }

    private void setScFile(String fileName) throws ParseException {
        File file = new File(fileName);
        if (!(file.exists() && file.isFile()))
            throw new ParseException("Slicing criterion file is not an existing file.");
        scFile = file;
    }

    private void setScLine(int line) throws ParseException {
        if (line <= 0)
            throw new ParseException("The line of the slicing criterion must be strictly greater than zero.");
        scLine = line;
    }

    private void setScVar(String scVar) {
        this.scVar = scVar;
    }

    public Set<File> getDirIncludeSet() {
        return Collections.unmodifiableSet(dirIncludeSet);
    }

    public File getOutputDir() {
        return outputDir;
    }

    public File getScFile() {
        return scFile;
    }

    public int getScLine() {
        return scLine;
    }

    public String getScVar() {
        return scVar;
    }

    public void slice() throws ParseException {
        // Configure JavaParser
        StaticTypeSolver.addTypeSolverJRE();
        for (File directory : dirIncludeSet)
            StaticTypeSolver.addTypeSolver(new JavaParserTypeSolver(directory));
        StaticJavaParser.getConfiguration().setAttributeComments(false);

        // Build the SDG
        Set<CompilationUnit> units = new NodeHashSet<>();
        try {
            for (File file : dirIncludeSet) {
                if (file.isDirectory())
                    for (File f : (Iterable<File>) findAllJavaFiles(file)::iterator)
                        units.add(StaticJavaParser.parse(f));
                else
                    units.add(StaticJavaParser.parse(file));
            }
            units.add(StaticJavaParser.parse(scFile));
        } catch (FileNotFoundException e) {
            throw new ParseException(e.getMessage());
        }

        SDG sdg;
        switch (cliOpts.getOptionValue("type", "JSysDG")) {
            case "SDG":   sdg = new SDG();   break;
            case "ASDG":  sdg = new ASDG();  break;
            case "PSDG":  sdg = new PSDG();  break;
            case "ESSDG": sdg = new ESSDG(); break;
            case "JSysDG": sdg = new JSysDG(); break;
            default:
                throw new IllegalArgumentException("Unknown type of graph. Available graphs are SDG, ASDG, PSDG, ESSDG");
        }
        sdg.build(new NodeList<>(units));

        // Slice the SDG
        SlicingCriterion sc = new FileLineSlicingCriterion(scFile, scLine, scVar);
        Slice slice = sdg.slice(sc);

        // Convert the slice to code and output the result to `outputDir`
        for (CompilationUnit cu : slice.toAst()) {
            if (cu.getStorage().isEmpty())
                throw new IllegalStateException("A synthetic CompilationUnit was discovered, with no file associated to it.");
            String packagePath = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("").replace(".", "/");
            File packageDir = new File(outputDir, packagePath);
            packageDir.mkdirs();
            File javaFile = new File(packageDir, cu.getStorage().get().getFileName());
            try (PrintWriter pw = new PrintWriter(javaFile)) {
                pw.print(new BlockComment(getDisclaimer(cu.getStorage().get())));
                pw.print(cu);
            } catch (FileNotFoundException e) {
                System.err.println("Could not write file " + javaFile);
            }
        }
    }

    protected Stream<File> findAllJavaFiles(File directory) {
        Stream.Builder<File> builder = Stream.builder();
        findAllJavaFiles(directory, builder);
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
                builder.add(f);
        }
    }

    protected String getDisclaimer(CompilationUnit.Storage s) {
        return String.format("\n\tThis file was automatically generated as part of a slice with criterion" +
                        "\n\tfile: %s, line: %d, variable: %s\n\tOriginal file: %s\n",
                scFile, scLine, scVar, s.getPath());
    }

    protected void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp("java -jar sdg-cli.jar", HELP_HEADER, OPTIONS, "", true);
        System.exit(0);
    }

    public static void main(String... args) {
        try {
            new Slicer(args).slice();
        } catch (ParseException e) {
            System.err.println("Error parsing the arguments!\n" + e.getMessage());
        }
    }
}
