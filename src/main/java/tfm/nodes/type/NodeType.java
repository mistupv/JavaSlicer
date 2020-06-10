package tfm.nodes.type;

public enum NodeType {
    /** An instruction in the program. Statements, predicates and
     *  pseudo-predicates are included in this {@link NodeType}. */
    STATEMENT,
    /** The <emph>Enter</emph> node of a method. */
    METHOD_ENTER,
    /** The <emph>Exit</emph> node of a method. */
    METHOD_EXIT,
    /** A method call, that is contained in a {@link #STATEMENT} node. */
    METHOD_CALL,
    /** An argument or globally accesible variable that
     *  has been used in a method call. */
    ACTUAL_IN,
    /** An argument or globally accessible variable that
     *  has been modified in a method call. */
    ACTUAL_OUT,
    /** An argument or globally accessible variable that
     *  has been used in a method declaration. */
    FORMAL_IN,
    /** An argument or globally accessible variable that
     *  has been modified in a method declaration. */
    FORMAL_OUT,
    /** A node representing the return value of a non-void method call. */
    METHOD_CALL_RETURN,
    /** A node representing the return value of a non-void method declaration. */
    METHOD_OUTPUT
}
