package tfm.nodes.io;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

/** A formal-in or formal-out node, displaying interprocedural data dependencies. */
public class FormalIONode extends IONode<CallableDeclaration<?>> {
    protected FormalIONode(CallableDeclaration<?> astNode, ResolvedType varType, String varName, boolean isInput) {
        this(createLabel(isInput, varType, varName), astNode, varType, varName, isInput);
    }

    protected FormalIONode(String text, CallableDeclaration<?> astNode, ResolvedType varType, String varName, boolean isInput) {
        super(text, astNode, varType, varName, isInput);
    }

    protected static String createLabel(boolean isInput, ResolvedType varType, String varName) {
        if (isInput)
            return String.format("%s %s = %2$s_in", varType.describe(), varName);
        else
            return String.format("%s %s_out = %2$s", varType.describe(), varName);
    }

    public static FormalIONode createFormalIn(CallableDeclaration<?> declaration, ResolvedValueDeclaration resolvedValue) {
        return new FormalIONode(declaration, resolvedValue.getType(), resolvedValue.getName(), true);
    }

    public static FormalIONode createFormalOut(CallableDeclaration<?> declaration, ResolvedValueDeclaration resolvedValue) {
        return new FormalIONode(declaration, resolvedValue.getType(), resolvedValue.getName(), false);
    }

    public static FormalIONode createFormalInDecl(CallableDeclaration<?> declaration, ResolvedValueDeclaration resolvedValue) {
        ResolvedType type = resolvedValue.getType();
        String name = resolvedValue.getName();
        return new FormalIONode(type.describe() + " " + name, declaration, type, name, true);
    }
}
