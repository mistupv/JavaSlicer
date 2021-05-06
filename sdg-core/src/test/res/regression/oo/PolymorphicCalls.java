class Test{
    public static void main(String args[]) {
        A a;
        if (Math.random() > 0)
            a = new A(10);
        else
            a = new B(5,1);
        int i = a.f();
        int j = a.g();
        int k = a.h();
    }
}

class A {
    int x;

    public A(int a) {
        this.x = a;
    }

    public int f() {
        return x;
    }

    public int g() {
        return x;
    }

    public int h() {
        return x;
    }
}

class B extends A {
    int y;

    public B(int a, int b) {
        super(a);
        y = b;
    }

    public int f() {
        return y;
    }

    public int g() {
        return super.g() + y;
    }
}
