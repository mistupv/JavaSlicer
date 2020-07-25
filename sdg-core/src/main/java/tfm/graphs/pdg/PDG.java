package tfm.graphs.pdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.graphs.GraphWithRootNode;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.nodes.VariableAction;
import tfm.nodes.type.NodeType;

/**
 * The <b>Program Dependence Graph</b> represents the statements of a method in
 * a graph, connecting statements according to their {@link ControlDependencyArc control}
 * and {@link DataDependencyArc data} relationships. You can build one manually or use
 * the {@link Builder PDGBuilder}.
 * The variations of the PDG are represented as child types.
 */
public class PDG extends GraphWithRootNode<MethodDeclaration> {
    protected CFG cfg;

    public PDG() {
        this(new CFG());
    }

    public PDG(CFG cfg) {
        super();
        this.cfg = cfg;
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(VariableAction src, VariableAction tgt) {
        DataDependencyArc arc;
        if (src instanceof VariableAction.Definition && tgt instanceof VariableAction.Usage)
            arc = new DataDependencyArc((VariableAction.Definition) src, (VariableAction.Usage) tgt);
        else if (src instanceof VariableAction.Declaration && tgt instanceof VariableAction.Definition)
            arc = new DataDependencyArc((VariableAction.Declaration) src, (VariableAction.Definition) tgt);
        else
            throw new UnsupportedOperationException("Unsupported combination of VariableActions");
        addEdge(src.getGraphNode(), tgt.getGraphNode(), arc);
    }

    public CFG getCfg() {
        return cfg;
    }

    @Override
    public void build(MethodDeclaration method) {
        createBuilder().build(method);
        built = true;
    }

    protected Builder createBuilder() {
        return new Builder();
    }

    /**
     * Populates a {@link PDG}, given a complete {@link CFG}, an empty {@link PDG} and an AST root node.
     * For now it only accepts {@link MethodDeclaration} as root, as it can only receive a single CFG.
     * <br/>
     * <b>Usage:</b>
     * <ol>
     *     <li>Create an empty {@link CFG}.</li>
     *     <li>Create an empty {@link PDG} (optionally passing the {@link CFG} as argument).</li>
     *     <li>Create a new {@link Builder}, passing both graphs as arguments.</li>
     *     <li>Accept the builder as a visitor of the {@link MethodDeclaration} you want to analyse using
     *     {@link com.github.javaparser.ast.Node#accept(com.github.javaparser.ast.visitor.VoidVisitor, Object) Node#accept(VoidVisitor, Object)}:
     *     {@code methodDecl.accept(builder, null)}</li>
     *     <li>Once the previous step is finished, the complete PDG is saved in
     *     the object created in the second step. The builder should be discarded
     *     and not reused.</li>
     * </ol>
     */
    public class Builder {
        protected Builder() {
            assert PDG.this.getCfg() != null;
        }

        public void build(MethodDeclaration methodDeclaration) {
            if (methodDeclaration.getBody().isEmpty())
                throw new IllegalStateException("Method needs to have a body");
            buildAndCopyCFG(methodDeclaration);
            buildControlDependency();
            buildDataDependency();
        }

        protected void buildAndCopyCFG(MethodDeclaration methodDeclaration) {
            if (!cfg.isBuilt())
                cfg.build(methodDeclaration);
            cfg.vertexSet().stream()
                    .filter(node -> node.getNodeType() != NodeType.METHOD_EXIT)
                    .forEach(PDG.this::addVertex);
            assert cfg.getRootNode().isPresent();
            PDG.this.setRootNode(cfg.getRootNode().get());
        }

        protected void buildControlDependency() {
            new ControlDependencyBuilder(cfg, PDG.this).build();
        }

        protected void buildDataDependency() {
            for (GraphNode<?> node : vertexSet()) {
                for (VariableAction varAct : node.getVariableActions()) {
                    if (varAct.isUsage()) {
                        VariableAction.Usage use = (VariableAction.Usage) varAct;
                        for (VariableAction.Definition def : cfg.findLastDefinitionsFrom(node, use))
                            addDataDependencyArc(def, use);
                    } else if (varAct.isDefinition()) {
                        VariableAction.Definition def = (VariableAction.Definition) varAct;
                        for (VariableAction.Declaration dec : cfg.findLastDeclarationsFrom(node, def))
                            if (def.getGraphNode() != dec.getGraphNode())
                                addDataDependencyArc(dec, def);
                    }
                }
            }
        }
    }
}
