package tfm.programs.sdg;

import tfm.utils.Logger;

public class Example1 {

    /*
    public Example1() {

    }

     */

    int num;

    public static void main(String[] args) {
        int n1 = 1;
        int n2 = 2;

//        Example1 example1 = new Example1();
//        Example1 example2 = new Example1();

        int f = sum(sum(n1, n2), n2);

        Logger.log(f);
        Logger.log(z);
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
