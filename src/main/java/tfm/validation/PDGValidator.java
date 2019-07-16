package tfm.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.VoidType;
import tfm.graphs.PDGGraph;
import tfm.nodes.Node;
import tfm.utils.Utils;
import tfm.visitors.PDGCFGVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;

public class PDGValidator {

    private static final String PROGRAM_FOLDER = Utils.PROGRAMS_FOLDER + "pdg";
    private static final String PROGRAM_NAME = "Example2";

    public static void main(String[] args) throws FileNotFoundException {
        JavaParser.getStaticConfiguration().setAttributeComments(false);

        CompilationUnit originalProgram = JavaParser.parse(new File(String.format("%s/%s.java", PROGRAM_FOLDER, PROGRAM_NAME)));

        PDGGraph graph = new PDGGraph();

        originalProgram.accept(new PDGCFGVisitor(graph), graph.getRootNode());

//        graph.depthFirstSearch(graph.getRootNode(), new NodeVisitor<PDGNode>() {
//            @Override
//            public void visit(PDGNode node) {
//                if (node.equals(graph.getRootNode()))
//                    return;
//
//                Logger.log(node);
//
//                methodBody.addStatement(node.get);
//            }
//        });

        printPDGProgram("Generated" + PROGRAM_NAME, graph);
    }

    public static void printPDGProgram(String fileName, PDGGraph graph) throws FileNotFoundException {
        CompilationUnit generatedProgram = new CompilationUnit();
        ClassOrInterfaceDeclaration clazz = generatedProgram.addClass(fileName).setPublic(true);

        MethodDeclaration methodDeclaration = clazz.addMethod("main", Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        methodDeclaration.setType(new VoidType());

        Parameter parameter = new Parameter(
                new ArrayType(JavaParser.parseClassOrInterfaceType("String")),
                "args"
        );

        methodDeclaration.setParameters(new NodeList<>(Arrays.asList(parameter)));

        BlockStmt methodBody = new BlockStmt();
        methodDeclaration.setBody(methodBody);

        graph.getNodesAtLevel(1).stream()
                .sorted(Comparator.comparingInt(Node::getId))
                .forEach(node -> methodBody.addStatement(node.getAstNode()));

        PrintWriter printWriter = new PrintWriter(new File(String.format("out/%s.java", fileName)));

        printWriter.print(clazz.toString());
        printWriter.close();
    }
}
