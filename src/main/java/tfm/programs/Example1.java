package tfm.programs;

import tfm.utils.Logger;

public class Example1 {

    public static void main(String[] args) {
        int x = 1;
        int y = 2;

//        if (x < y) {
            while (x < y) {
//                y = x;

                while(y < x) {
                    y += x;
                    x = y;

                    while (x > 1) {
                        y += 12;
                        x = y;
                    }
                }

                x++;
            }

            y = x + 1;
//        } else {
//            x = 4;
//            y *= x;
//        }

        int e = (Integer) x;

//        switch (x) {
//            case 1:
//                e = 2;
//                break;
//            case 2:
//                e = 3;
//        }

        Logger.log(y);
    }
}
