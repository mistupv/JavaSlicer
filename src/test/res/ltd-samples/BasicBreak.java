package mytest;

public class BasicBreak {
	public static void main(String[] args) {
		int x = 0;
		bucle:
		while (true) {
			x++;
			for (int y = 0; y < 10; y++) {
				if (y == x) break;
				if (y * 2 == x) break bucle;
			}
			if (x > 10) break;
			x++;
		}
	}
}
