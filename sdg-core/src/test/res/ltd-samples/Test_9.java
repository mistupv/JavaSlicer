package ejemplos;

public class Test_9 {


	public static void main(String[] args) {
		// ANIDAMIENTO de IF y WHILE

		// ANIDAMIENTO de IF y WHILE 2

		int x = 0;
		if (x > 1) {
			x = 1;
			while (x > 2) {
				x = 2;
				while (x > 3) {
					x = 3;
					if (x > 4) {
						x = 4;
						if (x > 5) {
							x = 5;
						}
						x--;
					}
					x--;
				}
				x--;
			}
			x--;
		}

		x--;

	}
}
