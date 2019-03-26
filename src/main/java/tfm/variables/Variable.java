package tfm.variables;

import tfm.utils.Scope;

public class Variable<T> {

    private Scope scope;
    private String name;
    private T value;

    public Variable(Scope scope, String name) {
        this(scope, name, null);
    }

    public Variable(Scope scope, String name, T value) {
        this.scope = scope;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public int hashCode() {
        return scope.hashCode() + name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Variable)) {
            return false;
        }

        Variable other = (Variable) o;

        return name.equals(other.name) && scope.equals(other.scope);
    }

    @Override
    public String toString() {
        return String.format("Variable %s defined in scope %s", name, scope);
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
