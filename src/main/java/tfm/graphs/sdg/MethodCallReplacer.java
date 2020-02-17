package tfm.graphs.sdg;

import tfm.nodes.GraphNode;
import tfm.utils.Context;
import tfm.utils.Logger;

import java.util.Optional;

class MethodCallReplacer {

    private SDG sdg;

    public MethodCallReplacer(SDG sdg) {
        this.sdg = sdg;
    }

    public void replace() {
        this.sdg.getContexts().stream()
            .filter(context -> context.getCurrentMethod().isPresent())
            .forEach(context -> {
                Logger.log("MethodCallReplacer", context);

                Optional<GraphNode<?>> optionalRootNode = this.sdg.getRootNode(context);

                if (!optionalRootNode.isPresent()) {
                    return; // We don't have visited the code (e.g. the MethodDeclaration for a method call)
                }

                optionalRootNode.get().getAstNode().accept(new MethodCallReplacerVisitor(), context);
            });
    }
}
