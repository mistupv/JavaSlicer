package tfm.variables;

import tfm.nodes.Node;

public class VariableDependency {

    private Variable dependency;
    private Node node;

    public VariableDependency(Variable dependency, Node node) {
        this.dependency = dependency;
        this.node = node;
    }


    public Variable getDependency() {
        return dependency;
    }

    public Node getNode() {
        return node;
    }
}
