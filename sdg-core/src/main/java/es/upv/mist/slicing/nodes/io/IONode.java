package es.upv.mist.slicing.nodes.io;

import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.nodes.SyntheticNode;

import java.util.LinkedList;
import java.util.Objects;

/** A node representing an input or output from a declaration or call (formal or actual). */
public abstract class IONode<T extends Node> extends SyntheticNode<T> {
    protected final boolean isInput;
    protected final ResolvedType variableType;
    protected final String variableName;

    protected IONode(String instruction, T astNode, ResolvedType variableType, String variableName, boolean isInput) {
        super(instruction, astNode, new LinkedList<>());
        this.variableType = variableType;
        this.variableName = variableName;
        this.isInput = isInput;
    }

    public boolean isInput() {
        return isInput;
    }

    public boolean isOutput() {
        return !isInput;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof IONode
                && ((IONode<?>) o).isInput == isInput
                && Objects.equals(((IONode<?>) o).variableType, variableType)
                && Objects.equals(((IONode<?>) o).variableName, variableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isInput, variableType, variableName);
    }
}
