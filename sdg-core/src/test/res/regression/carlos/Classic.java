import java.util.Scanner;

public class Classic {
    public static void main(String[] args) {
        int sum = 0;
        int product = 1;
        int w = 7;
        int N = 10;
        for (int i = 1; i < N; i++) {
            sum = sum + i + w;
            product *= i;
        }
        System.out.println(sum);
        System.out.println(product);
    }

    public static void main2(String[] args) {
        String text = new Scanner(System.in).nextLine();
        int n = new Scanner(System.in).nextInt();
        int lines = 1;
        int chars = 1;
        String subtext = "";
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n') {
                lines += 1;
                chars += 1;
            } else {
                chars += 1;
                if (n != 0) {
                    subtext = subtext + c;
                    n -= 1;
                }
            }
            i++;
        }
        System.out.println(lines);
        System.out.println(chars);
        System.out.println(subtext);
    }
}
