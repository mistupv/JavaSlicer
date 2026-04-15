public class TestInlineCall {

    public static void main(String[] args) {
        f();
        f();
    }

    public static void f() {
        int x = 1;
    }
}