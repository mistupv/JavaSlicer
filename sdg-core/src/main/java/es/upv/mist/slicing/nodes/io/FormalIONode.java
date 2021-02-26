package es.upv.mist.slicing.nodes.io;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;

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

    public static FormalIONode createFormalIn(CallableDeclaration<?> declaration, ResolvedValueDeclaration resolvedValue) {
        return new FormalIONode(declaration, resolvedValue.getName(), true);
    }

    public static FormalIONode createFormalOut(CallableDeclaration<?> declaration, ResolvedValueDeclaration resolvedValue) {
        return new FormalIONode(declaration, resolvedValue.getName(), false);
    }

    public static FormalIONode createFormalInDecl(CallableDeclaration<?> declaration, ResolvedValueDeclaration resolvedValue) {
        String name = resolvedValue.getName();
        return new FormalIONode(name, declaration, name, true);
    }
}
