package tfm.jacosro.graphs;

import tfm.jacosro.nodes.CFGNode;

public abstract class CFGGraph<T> extends Graph<T, CFGNode<T>> {

    public CFGGraph() {
        super();

        nodes.add(new CFGNode<>(getNextNodeId(), getStartNodeData()));
    }

    protected abstract T getStartNodeData();

    @Override
    public CFGNode<T> addNode(T data) {
        CFGNode<T> newNode = new CFGNode<>(getNextNodeId(), data);
        nodes.add(newNode);

        return newNode;
    }

    public void addControlFlowArc(CFGNode<T> from, CFGNode<T> to) {
        to.controlFlowArcFrom(from);
        from.controlFlowArcTo(to);
    }
}
