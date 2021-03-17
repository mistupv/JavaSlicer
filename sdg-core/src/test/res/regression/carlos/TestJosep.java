public class TestJosep {
    static int x = 0;
    public static void main(int y) {
        int z = 0;
        while (y > z) {
            y--;
            if (x > 10) {
                log(x);
            } else {
                log(z);
            }

        }
        switch (z) {
            case 1:
                x--;
                break;
            case 2:
                x++;
            default:
                log(x);
                break;
            case 3:
                y = x + z;
                break;
        }
        log(x);
        log(y);
        log(z);
    }

//    public void objetos() {
//        Object o = new Object();
//        o.x = 10;
//        Object y = new Object();
//        y.z = 210;
//        o = y;
//        log(o);
//        log(y);
//    }

    public static void log(Object o) {}
}
