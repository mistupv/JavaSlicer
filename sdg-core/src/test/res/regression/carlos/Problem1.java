public class Problem1 {
    static boolean X = true, Y = true, Z = true;

    public static void main(String[] args) {
        while (X) {
            if (Y) {
                if (Z) {
                    System.out.println("A");
                    break;
                }
                System.out.println("B");
                break;
            }
            System.out.println("C");
        }
        System.out.println("D");
    }
}
