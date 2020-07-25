package tfm.graphs.sdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.utils.Context;

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
