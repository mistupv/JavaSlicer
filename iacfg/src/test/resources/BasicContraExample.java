public class BasicContraExample {

    public static void main(String[] args) {
        f();
        while(true) {
            f();
        }
    }

    public static void f() {
        int x = 1;
    }
}