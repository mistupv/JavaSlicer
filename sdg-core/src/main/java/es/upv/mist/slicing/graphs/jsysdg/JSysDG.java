package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import es.upv.mist.slicing.arcs.pdg.StructuralArc;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ExceptionSensitiveCallConnector;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.oo.MemberNode;
import es.upv.mist.slicing.slicing.JSysDGSlicingAlgorithm;
import es.upv.mist.slicing.slicing.SlicingAlgorithm;
import es.upv.mist.slicing.utils.NodeHashSet;

import java.util.NoSuchElementException;

public class JSysDG extends ESSDG {
    @Override
    protected SlicingAlgorithm createSlicingAlgorithm() {
        return new JSysDGSlicingAlgorithm(this);
    }

    @Override
    protected JSysDG.Builder createBuilder() {
        return new JSysDG.Builder();
    }

    /** Populates an ESSDG, using ESPDG and ESCFG as default graphs.
     * @see PSDG.Builder
     * @see ExceptionSensitiveCallConnector */
    class Builder extends ESSDG.Builder {
        protected NodeHashSet<ConstructorDeclaration> newlyInsertedConstructors = new NodeHashSet<>();

        @Override
        public void build(NodeList<CompilationUnit> nodeList) {
            insertImplicitConstructors(nodeList);
            super.build(nodeList);
        }

        @Override
        protected void createClassGraph(NodeList<CompilationUnit> nodeList) {
            super.createClassGraph(nodeList);
            insertTypeNodes();
        }

        /** Create implicit constructors, and store them in a set so that they may be built with implicit nodes. */
        protected void insertImplicitConstructors(NodeList<CompilationUnit> nodeList) {
            nodeList.accept(new ModifierVisitor<>() {
                @Override
                public Visitable visit(ClassOrInterfaceDeclaration n, Object arg) {
                    if (n.getConstructors().isEmpty())
                        newlyInsertedConstructors.add(n.addConstructor(Modifier.Keyword.PUBLIC));
                    return super.visit(n, arg);
                }

                @Override
                public Visitable visit(EnumDeclaration n, Object arg) {
                    if (n.getConstructors().isEmpty())
                        newlyInsertedConstructors.add(n.addConstructor());
                    return super.visit(n, arg);
                }
            }, null);
        }

        @Override
        protected void buildCFG(CallableDeclaration<?> declaration, CFG cfg) {
            String origin;
            try {
                origin = " from " + declaration.findCompilationUnit().get().getStorage().get().getFileName() + ".";
            } catch (NoSuchElementException ignore) {
                origin = " (location unknown, may be synthetic).";
            }
            ((JSysCFG) cfg).build(declaration, newlyInsertedConstructors, ClassGraph.getInstance());
        }

        @Override
        protected CFG createCFG() {
            return new JSysCFG();
        }

        @Override
        protected PDG createPDG(CFG cfg) {
            assert cfg instanceof JSysCFG;
            return new JSysPDG((JSysCFG) cfg);
        }

        @Override
        protected void connectCalls() {
            new JSysCallConnector(JSysDG.this).connectAllCalls(callGraph);
            connectEnumToFormalIn();
        }

        protected void connectEnumToFormalIn() {
            for (GraphNode<?> g1 : vertexSet()) {
                if (!(g1.getAstNode() instanceof EnumDeclaration))
                    continue;
                VariableAction a1 = g1.getLastVariableAction();
                for (GraphNode<?> g2 : vertexSet()) {
                    if (g2 instanceof FormalIONode) {
                        FormalIONode fIn = (FormalIONode) g2;
                        if (fIn.isInput() && fIn.getVariableName().equals(a1.getName()))
                            a1.applySDGTreeConnection(JSysDG.this, g2.getLastVariableAction());
                    }
                }
            }
        }

        @Override
        protected void createSummaryArcs() {
            new SummaryArcAnalyzer(JSysDG.this, callGraph).analyze();
        }

        /** Adds type nodes (classes, interfaces, enums) to the SDG, along with their static fields. */
        protected void insertTypeNodes() {
            for (ClassGraph.Vertex<? extends TypeDeclaration<?>> cgVertex : ClassGraph.getInstance().typeVertices()) {
                String kind;
                if (cgVertex.getDeclaration() instanceof EnumDeclaration) {
                    kind = "enum";
                } else if (cgVertex.getDeclaration() instanceof ClassOrInterfaceDeclaration) {
                    if (((ClassOrInterfaceDeclaration) cgVertex.getDeclaration()).isInterface())
                        kind = "interface";
                    else
                        kind = "class";
                } else {
                    throw new IllegalStateException("Invalid kind of type node");
                }

                String typeName = cgVertex.getDeclaration().getNameAsString();
                GraphNode<?> typeNode = addVertex(kind + " " + typeName, cgVertex.getDeclaration());
                VariableAction typeDef = new VariableAction.Definition(VariableAction.DeclarationType.TYPE, typeName, typeNode);
                typeNode.addVariableAction(typeDef);

                for (FieldDeclaration fieldDecl : cgVertex.getDeclaration().getFields())
                    if (fieldDecl.isStatic())
                        for (VariableDeclarator vd : fieldDecl.getVariables())
                            typeNode.getLastVariableAction().getObjectTree().addStaticField(vd.getNameAsString(), fieldDecl);

                // Enums have additional static fields: their entries or constants
                if (cgVertex.getDeclaration() instanceof EnumDeclaration)
                    for (EnumConstantDeclaration ecDecl : ((EnumDeclaration) cgVertex.getDeclaration()).getEntries())
                        typeDef.getObjectTree().addStaticField(ecDecl.getNameAsString(), ecDecl);

                // Copy object tree nodes to the SDG
                addVertex(typeDef.getObjectTree().getMemberNode());
                addEdge(typeNode, typeDef.getObjectTree().getMemberNode(), new StructuralArc());
                for (MemberNode memberNode : typeDef.getObjectTree().nodeIterable()) {
                    addVertex(memberNode);
                    addEdge(memberNode.getParent(), memberNode, new StructuralArc());
                }
            }
        }
    }
}
