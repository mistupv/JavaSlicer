package tfm.graphs.sdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.graphs.GraphWithRootNode;
import tfm.nodes.GraphNode;
import tfm.utils.Context;
import tfm.utils.Logger;

import java.util.Optional;

class MethodCallReplacer {

    private SDG sdg;

    public MethodCallReplacer(SDG sdg) {
        this.sdg = sdg;
    }

    public void replace(Context context) {
        for (MethodDeclaration methodDeclaration : this.sdg.getMethodDeclarations()) {
            methodDeclaration.accept(new MethodCallReplacerVisitor(sdg), context);
        }
    }
}
