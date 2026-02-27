public class Test4 {

    public static void main(String[] args) {
        int x = 2;
        int resultado = f(g(h(x)));
        System.out.println("Resultado de f(g(h(" + x + "))): " + resultado);
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
}
