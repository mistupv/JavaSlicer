public class TestExamplePaper {

    public static int x = 0;

    public static void main(String[] args) {

        int y = 1;
        System.out.println("LLAMAMOS P");
        while (y < 10) {
            int z = 0;
            x += sum(x + y);
            z += sum(y + z);
            y++;
        }

        System.out.println("LLAMAMOS R");
        if (y == 10) {
            System.out.println("calculando fact1 " + y);
            int a = factorial(y);
            System.out.println("factorial1 " + a);
        } else {
            System.out.println("calculando fact2 " + y);
            int a = factorial(y);
            System.out.println("factorial2 " + a);
        }

        int a = 0;
        if (y < 10) {
            while (x > 0) {
                a = factorial(x);
                x--;
            }
        } else {
            while (x > 0) {
                a = factorial(x);
                x--;
            }
        }

        System.out.println("value a: " + a);
        System.out.println("LLAMAMOS L");
        res(a);

    }

    public int res(int a) {
        int value = 0;
        if (a == 5) {
            value += res2(a);
        }
        return value - a;
    }

    public int res2(int a) {
        int value = 0;
        if (a == 5) {
            value += res3(a);
        }
        return value - a;
    }

    public int res3(int a) {
        int value = 0;
        if (a == 5) {
            value += res(10);
        } else {
            value += a;
        }
        return value;
    }

    public int factorial(int n) {
        if (n != 1) {
            System.out.println("n: " + n);
            return n * factorial(n - 1);
        } else {
            System.out.println("n: " + 1);
            return 1;
        }

    }

    public int sum(int a) {
        int value = 0;
        value += sum2(a);
        return value;
    }

    public int sum2(int a) {
        int value2 = 0;
        value2 += sum3(a);
        value2 += sum3(a - 1);
        return value2;
    }

    public int sum3(int a) {
        int value3 = 0;
        while (a > 0) {
            value3 += a;
            a--;
        }
        return value3;
    }
}