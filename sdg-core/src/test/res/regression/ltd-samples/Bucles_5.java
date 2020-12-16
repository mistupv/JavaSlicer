package ejemplos;

public class Bucles_5 {

	public static void main(String[] args) {
		int x = 0;
		char y = '0';

		// BUCLE FOR anidado a otro FOR
		System.out.println("Empieza bucle FOR anidado a otro FOR:");
		for (x = 1; x <= 10; x++) {
			System.out.print(" " + x);
			for (y = 'a'; y <= 'c'; y++) {
				System.out.print(" " + y);
			}
		}
		System.out.println();

		// BUCLE WHILE anidado a otro WHILE
		System.out.println("Empieza bucle WHILE anidado a otro WHILE:");
		x = 1;
		while (x <= 10) {
			System.out.print(" " + x);
			y = 'a';
			while (y <= 'c') {
				System.out.print(" " + y);
				y++;
			}
			x++;
		}
		System.out.println();

		// BUCLE FOR anidado a bucle DO WHILE 		
		System.out.println("Empieza bucle FOR anidado a bucle DO WHILE:");
		x = 1;
		do {
			System.out.print(" " + x);
			for (y = 'a'; y <= 'c'; y++) {
				System.out.print(" " + y);
			}
			x++;
		}
		while (x <= 10);
		System.out.println();

	}
}
