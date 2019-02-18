package tfm.programs;

public class Example1 {

    public static void main(String[] args) {
        int x = 1;
        int y = 2;

        if (x < y) {
            while (x < y) {
                x++;
            }

            y = x + 1;
        } else {
            x = 4;
            y *= x;
        }

        System.out.println(y);
    }
}
