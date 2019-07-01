package tfm.programs;

public class WhileLoop {

    void while1() {
        int x = 1;
        int y = 2;

        while(x < 2) {

        }
    }

    void while2() {
        int x = 1;
        int y = 2;

        while(x < y) {
            x = y;
        }
    }

    void while3() {
        int x = 1;
        int y = 2;

        while(x < y) {
            x += y;
            y = 2;
        }
    }

    void while4() {
        int x = 1;
        int y = 2;

        while(x < y) {
            x += y;

            while (y < x) {
                y++;
            }
        }
    }

    void while5() {
        int x = 1;
        int y = 2;

        while(x < y) {
            x += y;

            while (y < x) {
                y++;
            }

            y += 1;
        }
    }
}
