# TFM

- [TFM](#tfm)
  - [Introduction](#introduction)
  - [Quick start](#quick-start)
    - [Build a graph](#build-a-graph)
    - [Slice a program](#slice-a-program)
  - [Structure](#structure)
    - [Summary](#summary)
  - [Current state](#current-state)
    - [Graphs](#graphs)
    - [Statements covered](#statements-covered)
  - [To do list](#to-do-list)
    - [SDG](#sdg)
    - [General](#general)
  - [Code samples](#code-samples)
    - [Build a CFG from a program](#build-a-cfg-from-a-program)
    - [Get a slice of the PDG of a program](#get-a-slice-of-the-pdg-of-a-program)
  - [Workflow](#workflow)

## Introduction

The main goal of this work is to develop a Java slicer. This is done by building a System Dependence Graph of the program being sliced

## Quick start

### Build a graph

Find `Main` class (`tfm/exec`), modify static fields of the class (the program being analyzed, the graph to build, etc.) and execute it. You will find the output in `tfm/out` as a png image

### Slice a program 

Find `Slice` class (`tfm/slicing`), set the program path and execute. The sliced program will be in `tfm/out`

## Structure

Graphs are built using a library called `graphlib`, located in `lib/graphlib.jar`. This library is old and has some issues I had to fix...

The main class is the `Graph` class, which extends from `graphlib`'s `Graph` class. This class includes some behaviour fixes, and some general interest methods (like `toString`, `toGraphvizRepresentation`, etc.)

Every graph has a set of nodes and arrows. `GraphNode` and `Arc` classes are used to represent them respectively.

A set of visitors is implemented for many things, such as graph building, data dependence building, etc... (available in `tfm/visitors`)

A bunch of programs are written in `tfm/programs`, you can write more there.

Some naive testing is implemented in the `tfm/validation` folder. Currently, a PDG can be compared with a program to check their equality.

Some util methods are available in `tfm/utils` (such as AST utils, logger, etc.)

Forget about the `tfm/scopes` folder, it was an idea I had to discard and it has to be deleted.

### Summary

- Graphs (`tfm/graphs`)
  - CFGGraph
  - PDGGraph
  - SDGGraph
  
- Nodes (`tfm/nodes`)
  - ~~CFGNode, PDGNode, SDGNode~~ (_Deprecated_)
  - GraphNode
  - MethodCallNode (_idk if this is necessary, maybe it can be deleted_)

- Arcs (`tfm/arcs`)
  - ControlFlowArc
  - DataDependencyArc
  - ControlDependencyArc

- Visitors (`tfm/visitors`)
  - CFGBuilder
  - ~~PDGVisitor~~ (_Deprecated, it was an intent to build a PDG with no CFG needed_)
  - PDGBuilder
  - ControlDependencyBuilder
  - DataDependencyBuilder
  - SDGBuilder (_Probably deprecated_)
  - NewSDGBuilder -**Work in progress**-
  - MethodCallReplacerVisitor (_Replaces method call nodes with in and out variable nodes_) -**Work in progress**-

## Current state

### Graphs

- CFG: Done!
- PDG: Done!
- SDG: PDGs are built for each method

### Statements covered

- Expressions (ExpressionStmt)
- If (IfStmt)
- While, DoWhile (WhileStmt, DoStmt)
- For, Foreach (ForStmt, ForeachStmt)
- Switch (SwitchStmt, SwitchEntryStmt)
- Break (BreakStmt)
- Continue (ContinueStmt)

## To do list

### SDG

- Replace method call nodes with in and out variables nodes and build arrows for them
- Build summary arrows

### General

- Switch to a (much) better graph library like [JGraphT](https://jgrapht.org/). It also supports graph visualization
- Performance review
- Make a test suite (test graph building, slicing, etc.)
- Add support to more Java language features (lambdas, etc.)

## Code samples

### Build a CFG from a program

```java
public CFGGraph buildCFG(File programFile) {
    JavaParser.getStaticConfiguration().setAttributeComments(false); // Always disable comments, just in case

    Node astRoot = JavaParser.parse(programFile);

    return Graphs.CFG.fromASTNode(astRoot); // Creates a new graph representing the program
}
```

### Get a slice of the PDG of a program

```java
public PDGGraph getSlice(File program, SlicingCriterion slicingCriterion) {
    JavaParser.getStaticConfiguration().setAttributeComments(false); // Always disable comments, just in case

    Node astRoot = JavaParser.parse(programFile);

    PDGGraph pdg = Graphs.PDG.fromASTNode(astRoot);

    return pdg.slice(slicingCriterion);
}

```

## Workflow

- Branches:
  - `master` (only for stable versions)
  - `develop` (main branch)
  - `<issue number>`

1. Discover a new feature/fix
2. Open an issue describing it and assign it
4. Create a new branch from `develop` with the same name as the issue number (e.g. for issue #12 the new branch is called `12`)
5. Write the solution to the issue
6. Once resolved, open a pull request from the issue branch to `develop` branch
7. Finally, when pull request is merged, remove branch