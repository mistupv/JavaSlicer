package mytest;

public class BasicContinue {
	public static void main(String[] args) {
		int x = 0;
		bucle:
		while (x < 20) {
			x++;
			for (int y = 0; y < 10; y++) {
				if (y == x) continue;
				if (y * 2 == x) continue bucle;
			}
			if (x > 10) continue;
			x++;
		}
	}
}
