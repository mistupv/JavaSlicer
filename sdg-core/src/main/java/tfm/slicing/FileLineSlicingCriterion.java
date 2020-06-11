package tfm.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.Statement;
import tfm.graphs.sdg.SDG;
import tfm.nodes.GraphNode;

import java.io.File;
import java.util.Optional;

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

    protected Optional<CompilationUnit> findCompilationUnit(NodeList<CompilationUnit> cus) {
        for (CompilationUnit cu : cus) {
            Optional<CompilationUnit.Storage> optStorage = cu.getStorage();
            if (optStorage.isPresent() && optStorage.get().getFileName().equals(file.getName())
                    && optStorage.get().getDirectory().equals(file.getParentFile().toPath()))
                return Optional.of(cu);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return file + "#" + lineNumber + ":" + variable;
    }
}
