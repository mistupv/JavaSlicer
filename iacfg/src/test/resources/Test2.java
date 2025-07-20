public class Test2 {

    public static void main(String[] args) {
        saludar();

        int resultado = sumar(5, 7);
        System.out.println("La suma es: " + resultado);

        mostrarParesHasta(10);

        int cuadrado = alCuadrado(4);
        System.out.println("El cuadrado de 4 es: " + cuadrado);
    }

    public static void saludar() {
        System.out.println("¡Hola desde el método saludar!");
    }

    public static int sumar(int a, int b) {
        return a + b;
    }

    public static void mostrarParesHasta(int limite) {
        System.out.println("Números pares hasta " + limite + ":");
        for (int i = 0; i <= limite; i++) {
            if (i % 2 == 0) {
                System.out.print(i + " ");
            }
        }
        System.out.println();
    }

    public static int alCuadrado(int n) {
        return n * n;
    }
}
