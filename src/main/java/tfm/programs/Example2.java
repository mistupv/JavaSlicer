package tfm.programs;

public class Example2 {

    public static void main(String[] args) {
        int x = 1;
        int y = 2;

        if (x > y)
            x = 1;

        int z = 10;

        for(int i = 0, o = 0; i < z; i++, o++) {
            z--;
        }
    }
}
