package tfm.programs.sdg;

public class Example1 {

    public static void main(String[] args) {
        int n1 = 1;
        int n2 = 2;
        
        int f = sum(n1, n2);
        
        System.out.println(f);
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
