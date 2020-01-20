package tfm.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.visitors.pdg.PDGBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class PDGValidator {

    private static final String PROGRAM_FOLDER = Utils.PROGRAMS_FOLDER + "pdg";
    private static final String PROGRAM_NAME = "Example2";
    private static final String METHOD_NAME = "main";

    public static void main(String[] args) throws FileNotFoundException {
        JavaParser.getStaticConfiguration().setAttributeComments(false);

        CompilationUnit originalProgram = JavaParser.parse(new File(String.format("%s/%s.java", PROGRAM_FOLDER, PROGRAM_NAME)));

        if (METHOD_NAME.isEmpty()) {
            originalProgram.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    Logger.format("On method: %s. Generating and comparing...", n.getNameAsString());
                    boolean check = generateAndCheck(n);

                    Logger.format("Result: %s", check ? "equal" : "not equal");
                }
            }, null);
        } else {
            Optional<MethodDeclaration> optionalTarget = originalProgram.findFirst(MethodDeclaration.class,
                    methodDeclaration -> Objects.equals(methodDeclaration.getNameAsString(), METHOD_NAME));

            if (!optionalTarget.isPresent()) {
                throw new RuntimeException(String.format("Method '%s' not found", METHOD_NAME));
            }

            Logger.format("On method: %s. Generating and comparing...", METHOD_NAME);

            boolean check = generateAndCheck(optionalTarget.get());

            Logger.format("Result: %s", check ? "equal" : "not equal");
        }
    }

    public static boolean generateAndCheck(MethodDeclaration methodDeclaration) {
        PDGGraph graph = new PDGGraph();

        methodDeclaration.accept(new PDGBuilder(graph), null);

        return check(methodDeclaration, graph);
    }

    public static boolean check(MethodDeclaration methodDeclaration, PDGGraph graph) {
        MethodDeclaration generatedMethod = generateMethod(methodDeclaration, graph);

        return ProgramComparator.areEqual(methodDeclaration, generatedMethod);
    }

    @Deprecated
    public static MethodDeclaration generateMethod(MethodDeclaration info, PDGGraph graph) {
        // TODO: this does not work properly, replace or remove
        throw new IllegalStateException("Deprecated method");
//        MethodDeclaration methodDeclaration = new MethodDeclaration();
//
//        methodDeclaration.setName(info.getNameAsString());
//        methodDeclaration.setModifiers(info.getModifiers());
//        methodDeclaration.setType(info.getType());
//        methodDeclaration.setParameters(info.getParameters());
//
//        BlockStmt methodBody = new BlockStmt();
//        methodDeclaration.setBody(methodBody);
//
//        graph.getNodesAtLevel(1).stream()
//                .sorted(Comparator.comparingInt(GraphNode::getId))
//                .forEach(node -> methodBody.addStatement((Statement) node.getAstNode()));
//
//        return methodDeclaration;
    }

    public static void printPDGProgram(String fileName, PDGGraph graph) throws FileNotFoundException {
        CompilationUnit generatedProgram = new CompilationUnit();
        ClassOrInterfaceDeclaration clazz = generatedProgram.addClass(fileName).setPublic(true);

        MethodDeclaration info = new MethodDeclaration();

        info.setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        info.setType(new VoidType());
        info.setName("main");

        Parameter parameter = new Parameter(
                new ArrayType(JavaParser.parseClassOrInterfaceType("String")),
                "args"
        );
        info.setParameters(new NodeList<>(parameter));

        MethodDeclaration generated = generateMethod(info, graph);

        clazz.addMember(generated);

        PrintWriter printWriter = new PrintWriter(new File(String.format("out/%s.java", fileName)));

        printWriter.print(clazz.toString());

        printWriter.close();
    }
}
