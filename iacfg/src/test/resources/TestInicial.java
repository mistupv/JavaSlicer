public class TestInicial {

    public static int z = 0;

    public static void main(String[] args) {
        int x = 1;
        boolean a = true;

        while (x < 100) {
            if (x < 50) {
                System.out.println(x);
                x = incrementar(x, 0);
            } else {
                System.out.println(x);
                x = incrementarBucle(x);
            }
        }
    }

    private static int incrementar(int a, int b) {
        b++;
        return a + 1;
    }

    private static int incrementarBucle(int a) {
        if (a > 0) {
            int x = 0;
            for (int i = 0; i < a; i++) {
                x = x + i;
            }
            return x;
        } else {
            return a;
        }
    }
}
