package tfm.nodes;

import com.github.javaparser.ast.Node;

import java.util.List;

/**
 * A synthetic node, which has not been generated from an instruction. Examples include
 * the exit node, actual-in, actual-out, formal-in, formal-out, call, declaration output, call return,
 * normal-exit, normal-return, exception-exit and exception-return.
 */
public abstract class SyntheticNode<T extends Node> extends GraphNode<T> {
    protected SyntheticNode(String instruction, T astNode) {
        super(instruction, astNode);
    }

    protected SyntheticNode(String instruction, T astNode, List<VariableAction> variableActions) {
        super(instruction, astNode, variableActions);
    }
}
