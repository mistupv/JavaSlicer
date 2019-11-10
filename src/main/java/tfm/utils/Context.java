package tfm.utils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.Objects;
import java.util.Optional;

public class Context {

    private CompilationUnit currentCU;
    private ClassOrInterfaceDeclaration currentClass;
    private MethodDeclaration currentMethod;

    public Context() {

    }

    public Context(CompilationUnit cu) {
        this(cu, null, null);
    }

    public Context(CompilationUnit cu, ClassOrInterfaceDeclaration clazz) {
        this(cu, clazz, null);
    }

    public Context(CompilationUnit cu, ClassOrInterfaceDeclaration clazz, MethodDeclaration method) {
        this.currentCU = cu;
        this.currentClass = clazz;
        this.currentMethod = method;
    }

    public Optional<CompilationUnit> getCurrentCU() {
        return Optional.ofNullable(currentCU);
    }

    public Optional<ClassOrInterfaceDeclaration> getCurrentClass() {
        return Optional.ofNullable(currentClass);
    }

    public Optional<MethodDeclaration> getCurrentMethod() {
        return Optional.ofNullable(currentMethod);
    }

    public void setCurrentCU(CompilationUnit currentCU) {
        this.currentCU = currentCU;
    }

    public void setCurrentClass(ClassOrInterfaceDeclaration currentClass) {
        this.currentClass = currentClass;
    }

    public void setCurrentMethod(MethodDeclaration currentMethod) {
        this.currentMethod = currentMethod;
    }

    @Override
    public int hashCode() {
        return getCurrentCU().map(Node::hashCode).orElse(0) +
                getCurrentClass().map(Node::hashCode).orElse(0) +
                getCurrentMethod().map(Node::hashCode).orElse(0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Context)) {
            return false;
        }

        Context other = (Context) obj;

        return Objects.equals(currentCU, other.currentCU) &&
                Objects.equals(currentClass, other.currentClass) &&
                Objects.equals(currentMethod, other.currentMethod);
    }

    @Override
    public String toString() {
        return String.format("Context{compilationUnit: %s, class: %s, method: %s}",
                getCurrentCU().map(Node::toString),
                getCurrentClass().map(Node::toString),
                getCurrentMethod().map(Node::toString)
        );
    }
}
