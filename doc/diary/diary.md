# TFM Diary

## 10/5/20

### Implemented

`out` nodes of method calls.

### What is left

#### Distinguish when an out node should exist

An `out` node shouldn't always exist. Primitive types cannot be modified in any way inside a method, while object types can.

An argument to a method call may be any kind of expression. In general, any literal expression, such as `1`, `null`, `""`, `true`, etc. will not have an `out` parameter. So we have to take care of the rest.

An `out` node should exist if there is a possibility to trace the value. And it only exists that possibility when the value comes from a **variable**

So, for the list of expressions that are not literal:
- `ArrayAccess (array[0])`: only if `array` is a variable
- `ArrayCreationExpr`: NO
- `ArrayInitializerExpr`: NO
- `BinaryExpr`: NO, it returns a value
- `CastExpr ((Cast) obj)`: only if `obj` is a variable
- `ClassExpr (obj.class)`: NO
- `ConditionalExpr (1?a:b)`: we'll have to check `a` and `b` expressions
- `FieldAccessExpr (obj.field)`: only if `obj` is a **variable** 
- `InstanceOfExpr`: NO
- `MethodCallExpr (foo.bar(x))`: NO
- `NameExpr`: YES
- `ObjectCreationExpr`: NO
- `SuperExpr`: NO
- `ThisExpr`: NO
- `UnaryExpr (a++)`: we'll have to check the operator (only `++`, `--` return the variable), and if `a` is a variable

## 11/5/20

### Implemented

- Added a CALL node between the node which contains the statement with the `MethodCallExpr` and the `MethodDeclaration`. This node is linked with `in` and `out` nodes. This is because if a statement has more than 1 `MethodCallExpr`, it may have repeated `in` and `out` nodes with the same variables
- Changed `out` node expression from `VariableDeclarationExpr` to `AssignExpr`, where the name of the variable is the argument expression

### WIP

- `OutNodeVariableVisitor`, a visitor that returns the expressions that should be candidate for being `out` nodes. This visitor overrides all `*Expr` methods who may contain a `NameExpr`
    - `ConditionalExpr`: Here, we have `ThenExpr` and `ElseExpr`. As we are performing a static analysis, both should be considered for `out` node in case they represented a variable (This is why `OutNodeVariableVisitor` uses a `List`, and doesn't just return a `String` representing the variable)

### Issues

- `Out` node is generated for `MethodCallExpr` as an argument

- When a `MethodCallExpr` is an argument of another, we have to link its output to the `in` node of the containing one