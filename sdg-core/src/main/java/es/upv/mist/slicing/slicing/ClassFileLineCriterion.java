package es.upv.mist.slicing.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.Objects;
import java.util.Optional;

public class ClassFileLineCriterion extends LineNumberCriterion {
  private final String fullyQualifiedClassName;

  public ClassFileLineCriterion(final String fullyQualifiedClassName, int lineNumber, String variable) {
    super(lineNumber, variable);
    this.fullyQualifiedClassName = fullyQualifiedClassName;
  }

  /** Locates the compilation unit that corresponds to this criterion's file. */
  protected Optional<CompilationUnit> findCompilationUnit(NodeList<CompilationUnit> cus) {
    for (CompilationUnit cu : cus) {
      if (cu.getTypes().stream()
              .map(TypeDeclaration::getFullyQualifiedName)
              .map(o -> o.orElse(null))
              .anyMatch(type -> Objects.equals(type, fullyQualifiedClassName)))
        return Optional.of(cu);
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return fullyQualifiedClassName + "#" + lineNumber + ":" + variable;
  }
}
