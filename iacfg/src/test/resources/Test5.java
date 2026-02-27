public class Test5 {

    public static void main(String[] args) {
        int x = 3;
        int resultado = f(g(x) + y(x)) + h(y(x));
        System.out.println("Resultado de f(g(x) + y(x)) + h(y(x)): " + resultado);
    }

    public static int g(int x) {
        return x * 2;
    }

    public static int y(int x) {
        return x + 4;
    }

    public static int f(int a) {
        return a - 1;
    }

    public static int h(int b) {
        return b * b;
    }
}
