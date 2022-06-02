module com.github.javaparser.symbolsolver {
    requires com.github.javaparser.core;
    requires com.google.common;
    requires javassist;
    exports com.github.javaparser.symbolsolver;
    exports com.github.javaparser.symbolsolver.javaparsermodel.declarations;
    exports com.github.javaparser.symbolsolver.model.resolution;
    exports com.github.javaparser.symbolsolver.resolution.typesolvers;
    exports com.github.javaparser.symbolsolver.model.typesystem;
}