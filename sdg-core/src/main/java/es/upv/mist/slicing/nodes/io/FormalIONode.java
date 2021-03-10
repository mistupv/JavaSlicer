package es.upv.mist.slicing.nodes.io;

import com.github.javaparser.ast.body.CallableDeclaration;

/** A formal-in or formal-out node, displaying interprocedural data dependencies. */
public class FormalIONode extends IONode<CallableDeclaration<?>> {
    protected FormalIONode(CallableDeclaration<?> astNode, String varName, boolean isInput) {
        this(createLabel(isInput, varName), astNode, varName, isInput);
    }

    protected FormalIONode(String text, CallableDeclaration<?> astNode, String varName, boolean isInput) {
        super(text, astNode, varName, isInput);
    }

    protected static String createLabel(boolean isInput, String varName) {
        if (isInput)
            return String.format("%s = %1$s_in", varName);
        else
            return String.format("%s_out = %1$s", varName);
    }

    public static FormalIONode createFormalIn(CallableDeclaration<?> declaration, String name) {
        return new FormalIONode(declaration, name, true);
    }

    public static FormalIONode createFormalOut(CallableDeclaration<?> declaration, String name) {
        return new FormalIONode(declaration, name, false);
    }

    public static FormalIONode createFormalInDecl(CallableDeclaration<?> declaration, String name) {
        return new FormalIONode(name, declaration, name, true);
    }
}
