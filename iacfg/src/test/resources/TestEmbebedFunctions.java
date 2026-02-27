public class TestEmbebedFunctions {

    public static int x = 0;
    public static int y = 5;
    public static int z = 7;

    public static void main(String[] args) {

        System.out.println("Valores iniciales:");
        System.out.println("x: " + TestEmbebedFunctions.x);
        System.out.println("y: " + TestEmbebedFunctions.y);
        System.out.println("z: " + TestEmbebedFunctions.z);

        TestEmbebedFunctions.x = 10;
        TestEmbebedFunctions.y = TestEmbebedFunctions.y + TestEmbebedFunctions.x;

        System.out.println("\nValores despues de modificar:");
        System.out.println("x: " + TestEmbebedFunctions.x);
        System.out.println("y: " + TestEmbebedFunctions.y);
        System.out.println("z: " + TestEmbebedFunctions.z);

        int valor1 = multiplyCtimes(TestEmbebedFunctions.x, incrementar(TestEmbebedFunctions.y, TestEmbebedFunctions.x), TestEmbebedFunctions.z);
        System.out.println("valor1 (x * (y+1) repeated z times): " + valor1);

        int valor2 = multiplyCtimes(TestEmbebedFunctions.x, incrementarBucle(TestEmbebedFunctions.y), TestEmbebedFunctions.z);
        System.out.println("valor2 (x * sum(0..y-1) repeated z times): " + valor2);

        int valor3 = multiplyCtimes(TestEmbebedFunctions.x, TestEmbebedFunctions.y, incrementar(TestEmbebedFunctions.z, TestEmbebedFunctions.x));
        System.out.println("valor3 (x * y repeated (z+1) times): " + valor3);

        int valor4 = multiplyCtimes(TestEmbebedFunctions.x, TestEmbebedFunctions.y, incrementarBucle(TestEmbebedFunctions.z));
        System.out.println("valor4 (x * y repeated sum(0..z-1) times): " + valor4);
    }

    private static int multiplyCtimes(int a, int b, int c) {

        if (c <= 0) {
            return 0;
        }

        int result = b;
        for (int i = 1; i < c; i++) {
            result *= a;
        }

        return result;
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