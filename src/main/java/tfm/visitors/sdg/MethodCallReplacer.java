package tfm.visitors.sdg;

import tfm.graphs.SDGGraph;

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
