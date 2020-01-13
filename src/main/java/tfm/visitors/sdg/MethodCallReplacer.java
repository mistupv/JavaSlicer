package tfm.visitors.sdg;

import tfm.graphs.SDG;

public class MethodCallReplacer {

    private SDG sdg;

    public MethodCallReplacer(SDG sdg) {
        this.sdg = sdg;
    }

    public void replace() {
        this.sdg.getContextPDGGraphMap()
                .forEach((context, pdgGraph) -> {
                    if (!context.getCurrentMethod().isPresent()) {
                        return; // Should NOT happen
                    }

                    context.getCurrentMethod().get().accept(new MethodCallReplacerVisitor(pdgGraph), context);
                });
    }
}
