package tfm.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.Statement;
import tfm.graphs.sdg.SDG;
import tfm.nodes.GraphNode;

import java.io.File;
import java.util.Optional;

/** A slicing criterion that allows the selection of a file and line. */
public class FileLineSlicingCriterion extends LineNumberCriterion {
    protected final File file;

    public FileLineSlicingCriterion(File file, int lineNumber) {
        super(lineNumber, null);
        this.file = file;
    }

    @Override
    public Optional<GraphNode<?>> findNode(SDG graph) {
        Optional<CompilationUnit> optCu = findCompilationUnit(graph.getCompilationUnits());
        if (optCu.isEmpty())
            return Optional.empty();
        return optCu.get().findFirst(Statement.class, this::matchesLine).flatMap(graph::findNodeByASTNode);
    }

    /** Locates the compilation unit that corresponds to this criterion's file. */
    protected Optional<CompilationUnit> findCompilationUnit(NodeList<CompilationUnit> cus) {
        for (CompilationUnit cu : cus) {
            Optional<CompilationUnit.Storage> optStorage = cu.getStorage();
            if (optStorage.isPresent() && optStorage.get().getFileName().equals(file.getName())
                    && optStorage.get().getDirectory().toAbsolutePath().equals(file.getParentFile().toPath().toAbsolutePath()))
                return Optional.of(cu);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return file + "#" + lineNumber + ":" + variable;
    }
}
