package tfm.utils;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Utils {

    public static final String PROGRAMS_FOLDER = "src/main/java/tfm/programs/";

    public static BlockStmt blockWrapper(Statement statement) {
        if (statement.isBlockStmt())
            return statement.asBlockStmt();

        return new BlockStmt(new NodeList<>(statement));
    }

    public static boolean isLoop(Statement statement) {
        return statement.isWhileStmt()
                || statement.isDoStmt()
                || statement.isForStmt()
                || statement.isForEachStmt();
    }

    public static Statement findFirstAncestorStatementFrom(Statement statement, Predicate<Statement> predicate) {
        if (predicate.test(statement)) {
            return statement;
        }

        if (!statement.getParentNode().isPresent()) {
            return new EmptyStmt();
        }

        return findFirstAncestorStatementFrom((Statement) statement.getParentNode().get(), predicate);
    }

    public static <E> List<E> emptyList() {
        return new ArrayList<>(0);
    }

    public static <E> Set<E> emptySet() {
        return new HashSet<>(0);
    }
}
