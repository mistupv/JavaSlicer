public class Test3 {

    // Variables globales estáticas
    static int contadorGlobal = 0;

    public static void main(String[] args) {
        // Variable local
        int contadorLocal = 10;

        System.out.println("Contador local: " + contadorLocal);
        System.out.println("Contador global estático: " + contadorGlobal);

        // Modificaciones
        contadorLocal += 5;
        contadorGlobal += 5;

        incrementarContadorGlobal();

        System.out.println("Después de modificar:");
        System.out.println("Contador local: " + contadorLocal);
        System.out.println("Contador global estático: " + contadorGlobal);

        procesar(3);
    }

    // Método estático que modifica variable global
    public static void incrementarContadorGlobal() {
        contadorGlobal++;
    }

    // Método estático con variable local
    public static void procesar(int n) {
        int resultadoLocal = 0;

        for (int i = 0; i < n; i++) {
            resultadoLocal += i;
            contadorGlobal++;
        }

        System.out.println("Resultado local en procesar: " + resultadoLocal);
        System.out.println("Contador global estático ahora: " + contadorGlobal);
    }
}
