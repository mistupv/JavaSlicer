package tfm.nodes;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;

public class MethodRootNode extends NodeWithInOutVariables<MethodDeclaration> {

    public MethodRootNode(int id, String representation, MethodDeclaration node) {
        super(id, representation, node);
    }

    public MethodRootNode(int id, String representation, @NonNull MethodDeclaration node, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        super(id, representation, node, declaredVariables, definedVariables, usedVariables);
    }
}
