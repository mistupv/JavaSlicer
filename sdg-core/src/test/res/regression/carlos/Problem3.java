public class Problem3 {
    public static int x;

    public static void main() throws Exception {
        x = 0;
        try {
            f();
        } catch (Exception e) {
            System.out.println("error");
        }
        x = 1;
        f();
    }

    public static void f() throws Exception {
        if (x % 2 == 0)
            throw new Exception("error!");
        System.out.println("x = " + x);
    }
}
