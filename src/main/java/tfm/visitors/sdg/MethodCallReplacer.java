package tfm.visitors.sdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.utils.Context;

public class MethodCallReplacer {

    private SDGGraph sdgGraph;

    public MethodCallReplacer(SDGGraph sdgGraph) {
        this.sdgGraph = sdgGraph;
    }

    public void replace() {
        this.sdgGraph.getContextPDGGraphMap()
                .forEach((context, pdgGraph) -> {
                    if (!context.getCurrentMethod().isPresent()) {
                        return; // Should NOT happen
                    }

                    context.getCurrentMethod().get().accept(new MethodCallReplacerVisitor(pdgGraph), context);
                });
    }
}
