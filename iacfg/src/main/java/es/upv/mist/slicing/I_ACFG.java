package es.upv.mist.slicing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.StaticTypeSolver;
import org.apache.commons.cli.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.CodeGenerationUtils;
import es.upv.mist.slicing.graphs.augmented.ACFG;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class I_ACFG extends SDG {

    protected static final Map<CallableDeclaration<?>, ACFG> acfgMap = ASTUtils.newIdentityHashMap();

    public static void main(String[] args) throws ParseException {
        String ruta = "/src/main/java/es/upv/mist/slicing/tests/";
        String fichero = "Test_Entrevista.java";

        I_ACFG iAcfg = new I_ACFG();
        iAcfg.generarACFG(ruta, fichero);
    }

    public void generarACFG(String ruta, String fichero) {
        NodeList<CompilationUnit> units = new NodeList<>();
        File file = new File(CodeGenerationUtils.mavenModuleRoot(I_ACFG.class)+ruta+fichero);
        CompilationUnit compilationUnit = null;

        StaticJavaParser.getConfiguration().setAttributeComments(false);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "Configuring JavaParser");
        StaticTypeSolver.addTypeSolverJRE();

        try {
            units.add(compilationUnit = StaticJavaParser.parse(file));
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró el archivo");
        }

        buildACFGs(units);

        System.out.println("TESTING: " + acfgMap.toString());
    }

    protected void buildACFGs(NodeList<CompilationUnit> nodeList) {
        nodeList.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration n, Void arg) {
                boolean isInInterface = n.findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::isInterface).orElse(false);
                if (n.isAbstract() || isInInterface)
                    return; // Allow abstract methods
                ACFG acfg = new ACFG();
                acfg.build(n);
                acfgMap.put(n, acfg);
                super.visit(n, arg);
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                boolean isInInterface = n.findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::isInterface).orElse(false);
                if (n.isAbstract() || isInInterface)
                    return; // Allow abstract methods
                ACFG acfg = new ACFG();
                acfg.build(n);
                acfgMap.put(n, acfg);
                super.visit(n, arg);
            }
        }, null);
    }

    protected Stream<File> findJavaFile(File file) {
        Stream.Builder<File> builder = Stream.builder();
        builder.accept(file);
        return builder.build();
    }
}