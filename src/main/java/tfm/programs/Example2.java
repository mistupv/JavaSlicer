package tfm.programs;

import java.util.Arrays;

public class Example2 {

    public static void main(String[] args) {
        int x = 1;
        int y = 2;

        if (x > y)
            x = 1;

        int z = 10;

        Iterable<Integer> integers = Arrays.asList(1, 2, 3);

        for(int i : integers) {
            System.out.println(i);
        }
    }
}
