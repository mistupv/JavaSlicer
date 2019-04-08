package tfm.utils;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

public class Utils {


    public static BlockStmt blockWrapper(Statement statement) {
        if (statement.isBlockStmt())
            return statement.asBlockStmt();

        return new BlockStmt(new NodeList<>(statement));
    }
}
