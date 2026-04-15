public class TestChainCall {

    public static void main(String[] args) {
        f();
        g();
    }

    public static void f() {
        g();
    }

    public static void g() {
        int x = 1;
    }
}