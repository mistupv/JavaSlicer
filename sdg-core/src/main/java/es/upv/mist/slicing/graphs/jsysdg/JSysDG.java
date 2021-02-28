package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import es.upv.mist.slicing.graphs.augmented.PSDG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESSDG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ExceptionSensitiveCallConnector;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.utils.NodeHashSet;

public class JSysDG extends ESSDG {

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

        /** Create implicit constructors, and store them in a set so that they may be built with implicit nodes. */
        protected void insertImplicitConstructors(NodeList<CompilationUnit> nodeList) {
            nodeList.accept(new ModifierVisitor<>() {
                @Override
                public Visitable visit(ClassOrInterfaceDeclaration n, Object arg) {
                    if (n.getConstructors().isEmpty())
                        newlyInsertedConstructors.add(n.addConstructor(Modifier.Keyword.PUBLIC));
                    return super.visit(n, arg);
                }
            }, null);
        }

        @Override
        protected void buildCFG(CallableDeclaration<?> declaration, CFG cfg) {
            ((JSysCFG) cfg).build(declaration, newlyInsertedConstructors);
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
    }
}
