package tfm.visitors;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGNode;
import tfm.scopes.Scope;
import tfm.scopes.ScopeHolder;
import tfm.scopes.VariableScope;
import tfm.variables.VariableExtractor;

public class LoopDataVisitor extends PDGVisitor {

    Scope<PDGNode> firstDeclarationScope;

    public LoopDataVisitor(PDGGraph graph, ScopeHolder<PDGNode> scopeHolder) {
        super(graph, scopeHolder);
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, ScopeHolder<PDGNode> scope) {
//        new VariableExtractor()
//                .setOnVariableDefinitionListener(variable -> {
//                    if (firstDeclarationScope != null) {
//
//                    }
//                }).setOnVariableUseListener(variable -> {
//                    expressionScope.addVariableUse(variable, expressionNode);
//
//                    Scope<PDGNode> searchScope = scope.isVariableDefined(variable) ? scope : globalScope;
//
//                    searchScope.getLastDefinitions(variable)
//                            .forEach(variableDefinition -> graph.addDataDependencyArc(
//                                    variableDefinition.getNode(),
//                                    expressionNode,
//                                    variable
//                            ));
//                })
//                .visit(expression);
    }
}
