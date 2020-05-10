package tfm.programs.sdg;

import tfm.utils.Logger;

public class Example1 {

    /*
    public Example1() {

    }

     */

    int num;

    public static void main(String[] args) {
        int x = 1;
        int y = 2;

        Example1 example1 = new Example1();
        Example1 example2 = new Example1();

        int f = sum(example1.getNum(), example2.num);

        Logger.log(example1.num);
        Logger.log(f);
    }

    public int getNum() {
        return num;
    }

    private static int sum(int x, int y) {
        int res = x + y;
        return res;
    }

    /*
    public int m1() {
        return 1;
    }

    public int m2() {
        return m1();
    }
     */
}
