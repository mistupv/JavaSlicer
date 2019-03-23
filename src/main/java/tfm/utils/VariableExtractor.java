package tfm.utils;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableExtractor {

    private Map<String, Object> declaredVariables;
    private List<String> usedVariables;

    private VariableExtractor() {
        declaredVariables = new HashMap<>();
        usedVariables = new ArrayList<>();
    }

//    public static VariableExtractor parse(Expression expression) {
//        VariableExtractor extractor = new VariableExtractor();
//
//        return extractor;
//    }

    public static VariableExtractor parse(AssignExpr assignExpr) {
        VariableExtractor variableExtractor = new VariableExtractor();


        assignExpr.getTarget().ifVariableDeclarationExpr(variableDeclarationExpr ->
                variableDeclarationExpr.getVariables().forEach(variableDeclarator -> {

                })
        );

        return variableExtractor;
    }

    public static VariableExtractor parse(VariableDeclarationExpr variableDeclarationExpr) {
        VariableExtractor variableExtractor = new VariableExtractor();

        // todo
        return variableExtractor;
    }
}
