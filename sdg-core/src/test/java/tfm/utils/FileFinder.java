package tfm.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FileFinder {
    private static final String TEST_FILES = "./src/test/res/";
    private static final String DOT_JAVA = ".java";

    public static Collection<Arguments> findFiles(File directory, String suffix) throws FileNotFoundException {
        Collection<Arguments> res = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files == null) return Collections.emptyList();
        for (File f : files) {
            if (f.getName().endsWith(suffix))
                for (MethodDeclaration m : methodsOf(f))
                    res.add(Arguments.of(f, m.getNameAsString(), m));
            if (f.isDirectory())
                res.addAll(findFiles(f, suffix));
        }
        return res;
    }

    public static Arguments[] findAllMethodDeclarations() throws FileNotFoundException {
        Collection<Arguments> args = findFiles(new File(TEST_FILES), DOT_JAVA);
        return args.toArray(new Arguments[0]);
    }

    private static List<MethodDeclaration> methodsOf(File file) throws FileNotFoundException {
        return JavaParser.parse(file).findAll(MethodDeclaration.class);
    }
}
