public class TestGlobalVariables {

    public static int x = 0;
    public static int y = 5;

    public static void main(String[] args) {

        System.out.println("Valores iniciales:");
        System.out.println("x: " + TestGlobalVariables.x);
        System.out.println("y: " + TestGlobalVariables.y);

        TestGlobalVariables.x = 10;
        TestGlobalVariables.y = TestGlobalVariables.y + TestGlobalVariables.x;

        System.out.println("\nValores despues de modificar:");
        System.out.println("x: " + TestGlobalVariables.x);
        System.out.println("y: " + TestGlobalVariables.y);

        while (TestGlobalVariables.x < 100) {
            if (TestGlobalVariables.x < 50) {
                System.out.println(TestGlobalVariables.x);
                TestGlobalVariables.x = incrementar(TestGlobalVariables.x, 0);
            } else {
                System.out.println(TestGlobalVariables.x);
                TestGlobalVariables.x = incrementarBucle(TestGlobalVariables.x);
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
