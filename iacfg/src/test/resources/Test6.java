public class Test6 {

    static int contadorGlobal = 0;

    public static void main(String[] args) {
        int x = 5;

        int resultado1 = f(g(h(x)));
        System.out.println("Resultado f(g(h(x))): " + resultado1);

        int resultado2 = f(g(x) + y(x)) + h(y(x));
        System.out.println("Resultado f(g(x) + y(x)) + h(y(x)): " + resultado2);

        System.out.println("Contador global antes: " + contadorGlobal);

        incrementarContadorGlobal();

        System.out.println("Contador global después: " + contadorGlobal);

        imprimirNumerosParesHasta(x);

        {
            int temp = 100;
            System.out.println("Variable local en bloque: " + temp);
        }

        contarConSaltos(7);

        return;
    }

    public static int h(int x) {
        return x + 1;
    }

    public static int g(int y) {
        return y * 2;
    }

    public static int f(int z) {
        return z - 3;
    }

    public static int y(int x) {
        return x + 4;
    }

    public static void incrementarContadorGlobal() {
        contadorGlobal++;
    }

    public static void imprimirNumerosParesHasta(int limite) {
        System.out.println("Números pares hasta " + limite + ":");
        for (int i = 0; i <= limite; i++) {
            if (i % 2 == 0) {
                System.out.print(i + " ");
            }
        }
        System.out.println();
    }

    public static void contarConSaltos(int max) {
        System.out.println("Contando con saltos hasta " + max);
        for (int i = 0; i < max; i++) {
            if (i == 3) continue;
            if (i == 6) break;
            System.out.print(i + " ");
        }
        System.out.println();
    }
}
