public class TestInlineFunctions {

    public static int x = 0;
    public static int y = 5;
    public static int z = 7;

    public static void main(String[] args) {

        System.out.println("Valores iniciales:");
        System.out.println("x: " + x);
        System.out.println("y: " + y);
        System.out.println("z: " + z);

        x = 10;
        y = y + x;

        System.out.println("\nValores despues de modificar:");
        System.out.println("x: " + x);
        System.out.println("y: " + y);
        System.out.println("z: " + z);

        int valor1 = incrementar(x, y) + incrementar(y, z) + incrementar(z, x);
        System.out.println("valor1: " + valor1);
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