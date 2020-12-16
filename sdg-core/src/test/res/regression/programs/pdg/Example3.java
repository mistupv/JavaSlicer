package tfm.programs.pdg;

import tfm.utils.Logger;

public class Example3 {

    public static void main(String[] args) {
        int n = 1;
        int i = 1;

        int sum = 0;

        int product = 1;

        while (i < n) {
            sum += i;
            product *= i;
            i++;
        }

        System.out.println(sum);
        System.out.println(product);
    }
}
