public class TestExamplePaperSimple {

    public static void main(String[] args) {

        System.out.println("LLAMAMOS P");
        while (1 > 2) {
            System.out.println("S1");
            p1();
            System.out.println("S2");
            p1();
            System.out.println("S3");
        }

        if (1 > 2) {
            System.out.println("S4");
            r1();
            System.out.println("S5");
        } else {
            System.out.println("S6");
            r1();
            System.out.println("S7");
        }

        if (20 < 10) {
            while (10 > 0) {
                r1();
                System.out.println("S9");
            }
        } else {
            while (10 > 0) {
                r1();
                System.out.println("S10");
            }
        }

        System.out.println("S11");
        l();
        System.out.println("S12");
    }

    public static void p1() { p2(); }
    public static void p2() { p3(); }
    public static void p3() {
        while (1 > 2) {
            System.out.println("P3!");
        }
    }

    public static void r1() {
        System.out.println("S20");
        if (21 > 0) {
            r1();
        }
        System.out.println("S23");
        System.out.println("S24");
    }

    public static void l() {
        if (1 > 0) {
            m();
            System.out.println("L3");
        }
    }

    public static void m() {
        if (1 > 0) {
            n();
            System.out.println("M3");
        }
    }

    public static void n() {
        if (1 > 0) {
            l();
            System.out.println("N3");
        }
    }
}