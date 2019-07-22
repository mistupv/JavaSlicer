package tfm.validation;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import java.io.File;
import java.io.FileNotFoundException;

public class ProgramComparator {

    public static boolean areEqual(File one, File two) throws FileNotFoundException {
        JavaParser.getStaticConfiguration().setAttributeComments(false);

        CompilationUnit cu1 = JavaParser.parse(one);
        CompilationUnit cu2 = JavaParser.parse(two);

        return areEqual(cu1, cu2);
    }

    public static boolean areEqual(Node one, Node two) {
        return one.equals(two);
    }
}
