package tfm.variables;

import com.github.javaparser.ast.expr.Expression;
import org.checkerframework.checker.nullness.qual.NonNull;

public class VariableExtractor {

    private OnVariableDeclarationListener onVariableDeclarationListener;
    private OnVariableDefinitionListener onVariableDefinitionListener;
    private OnVariableUseListener onVariableUseListener;

    public VariableExtractor() {

    }

    public VariableExtractor setOnVariableDeclarationListener(OnVariableDeclarationListener listener) {
        this.onVariableDeclarationListener = listener;
        return this;
    }

    public VariableExtractor setOnVariableDefinitionListener(OnVariableDefinitionListener listener) {
        this.onVariableDefinitionListener = listener;
        return this;
    }

    public VariableExtractor setOnVariableUseListener(OnVariableUseListener listener) {
        this.onVariableUseListener = listener;
        return this;
    }

    public void visit(@NonNull Expression expression) {
        new VariableVisitor() {
            @Override
            void onVariableUse(@NonNull String variable) {
                if (onVariableUseListener != null)
                    onVariableUseListener.onVariableUse(variable);
            }

            @Override
            void onVariableDefinition(@NonNull String variable) {
                if (onVariableDefinitionListener != null)
                    onVariableDefinitionListener.onVariableDefinition(variable);
            }

            @Override
            void onVariableDeclaration(@NonNull String variable) {
                if (onVariableDeclarationListener != null)
                    onVariableDeclarationListener.onVariableDeclaration(variable);
            }
        }.visit(expression);
    }

    @FunctionalInterface
    public interface OnVariableDeclarationListener {
        void onVariableDeclaration(String variable);
    }

    @FunctionalInterface
    public interface OnVariableDefinitionListener {
        void onVariableDefinition(String variable);
    }

    @FunctionalInterface
    public interface OnVariableUseListener {
        void onVariableUse(String variable);
    }
}

