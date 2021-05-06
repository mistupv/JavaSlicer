package es.upv.mist.slicing.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;

import java.io.File;
import java.util.Optional;

/** A slicing criterion that allows the selection of a file and line. */
public class FileLineSlicingCriterion extends LineNumberCriterion {
    protected final File file;

    public FileLineSlicingCriterion(File file, int lineNumber) {
        this(file, lineNumber, null);
    }

    public FileLineSlicingCriterion(File file, int lineNumber, String variable) {
        super(lineNumber, variable);
        this.file = file;
    }

    /** Locates the compilation unit that corresponds to this criterion's file. */
    @Override
    protected Optional<CompilationUnit> findCompilationUnit(NodeList<CompilationUnit> cus) {
        for (CompilationUnit cu : cus) {
            if (cu.getStorage().isEmpty())
                continue;
            CompilationUnit.Storage storage = cu.getStorage().get();
            if (storage.getDirectory().toAbsolutePath().equals(file.toPath().toAbsolutePath().getParent())
                    && storage.getFileName().equals(file.getName()))
                return Optional.of(cu);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return file + "#" + lineNumber + (variable != null ? ":" + variable : "");
    }
}
