public class TestThreeCall {

    public static void main(String[] args) {
        f();
        while(true) {
            f();
        }
        f();
    }

    public static void f() {
        int x = 1;
    }
}