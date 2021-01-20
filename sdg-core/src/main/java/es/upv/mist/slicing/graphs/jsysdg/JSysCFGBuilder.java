package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import es.upv.mist.slicing.graphs.augmented.ACFGBuilder;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.LinkedList;
import java.util.List;

public class JSysCFGBuilder extends ACFGBuilder {

    /** List of inserted super calls in Javaparser AST to process them as Implicit Nodes (@ImplicitNode)*/
    protected List<Node> methodInsertedInstructions = new LinkedList<>();

    protected JSysCFGBuilder(JSysCFG graph) {
        super(graph);
    }

    /** Esto se llama porque lo hemos insertado fantasma o porque existe. A continuacion se inserta el codigo dynInit */
    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Void arg) {

        // 1. Create new super call if not present
        if (methodInsertedInstructions.contains(n)){
            ImplicitNode node = new ImplicitNode(n.toString(), n); // TODO: implementar
            connectTo(node);
        }
        else {
            connectTo(n);
        }
        // 2. Insert dynamic class code
        ClassOrInterfaceDeclaration containerClass = ((JSysCFG) graph).getDeclarationClass();
        NodeList<BodyDeclaration<?>> dynInitList = ((JSysCFG) graph).getClassGraph().getDynInit(containerClass.getNameAsString());
        dynInitList.accept(this, arg);

    }

    @Override
    public void visit(FieldDeclaration n, Void arg){
        connectTo(n);
    }

    @Override
    public void visit(InitializerDeclaration n, Void arg){
        // TODO
    }


    @Override
    protected void visitCallableDeclaration(CallableDeclaration<?> callableDeclaration, Void arg) {
        graph.buildRootNode(callableDeclaration);
        hangingNodes.add(graph.getRootNode());

        // 1. Check if first is super (only if constructor)
        //      then, create and build super()
        if (callableDeclaration instanceof ConstructorDeclaration){
            ConstructorDeclaration declaration = (ConstructorDeclaration) callableDeclaration;
            if (!ASTUtils.constructorHasExplicitConstructorInvocation(declaration)){
                ExplicitConstructorInvocationStmt superCall =
                        new ExplicitConstructorInvocationStmt(null, null, false, null, new NodeList<>());
                methodInsertedInstructions.add(superCall);
                declaration.getBody().addStatement(0, superCall);
            }
        }

        ASTUtils.getCallableBody(callableDeclaration).accept(this, arg);
        returnList.stream().filter(node -> !hangingNodes.contains(node)).forEach(hangingNodes::add);

        MethodExitNode exit = new MethodExitNode(callableDeclaration);
        graph.addVertex(exit);
        addMethodOutput(callableDeclaration, exit);
        connectTo(exit);
    }
}
