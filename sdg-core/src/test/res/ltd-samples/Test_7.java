package ejemplos;


public class Test_7 {

	public static void main(String[] args) {
		int x = 1;

		if (x == 1) {
			x = 2;
		} else x = 3;
		x = 4;
		if (x == 2) {
			x = 5;
		} else if (x == 3) x = 6;
		x = 7;
	}
}
