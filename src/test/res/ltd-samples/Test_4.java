package ejemplos;

public class Test_4 {

	public static void main(String[] args) {
		int x = 1;

		if (x == 1) {
			x = 2;
			if (x >= 1) {
				x = 3;
				x = 4;
			}
		}
		x = 5;
		x = 6;
	}
}
