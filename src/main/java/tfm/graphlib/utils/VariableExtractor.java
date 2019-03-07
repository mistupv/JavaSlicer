package tfm.graphlib.utils;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import java.util.ArrayList;
import java.util.List;

public class VariableExtractor {

    private List<String> declaredVariables;
    private List<String> usedVariables;

    private VariableExtractor() {
        declaredVariables = new ArrayList<>();
        usedVariables = new ArrayList<>();
    }

    public static VariableExtractor parse(Expression expression) {
        VariableExtractor extractor = new VariableExtractor();
        extractor.parse(expression);

        return extractor;
    }

    private void parse(AssignExpr assignExpr) {

    }

    private void parse(VariableDeclarationExpr variableDeclarationExpr) {

    }
}
