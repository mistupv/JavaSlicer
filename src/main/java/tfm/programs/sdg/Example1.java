package tfm.programs.sdg;

import tfm.utils.Logger;

public class Example1 {

    public static void main(String[] args) {
        int x = 1;
        int y = 2;

        int f = sum(x, y);

        Logger.log(f);
    }

    private static int sum(int x, int y) {
        return x + y;
    }
}
