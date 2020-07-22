package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;
import tfm.nodes.type.NodeType;

import java.util.LinkedList;
import java.util.Objects;

public abstract class IONode<T extends Node> extends SyntheticNode<T> {
    protected Parameter parameter;

    protected IONode(NodeType type, String instruction, T astNode, Parameter parameter) {
        super(type, instruction, astNode, new LinkedList<>());
        this.parameter = Objects.requireNonNull(parameter);
    }

    public String getParameterName() {
        return parameter.getNameAsString();
    }

    public Parameter getParameter() {
        return parameter;
    }

    public Type getType() {
        return parameter.getType();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof IONode &&
                ((IONode<?>) o).parameter.equals(parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parameter);
    }
}
