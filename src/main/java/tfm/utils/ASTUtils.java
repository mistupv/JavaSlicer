package tfm.utils;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.function.Predicate;

public class ASTUtils {

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

    /**
     * Clones an entire AST by cloning the root node of the given node
     * @param node - a node of the AST
     * @return the root node of the cloned AST
     */
    public static Node cloneAST(Node node) {
        return node.findRootNode().clone();
    }
}
