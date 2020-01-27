package ejemplos;

public class Bucles_3 {

	public static void main(String[] args) {
		int x;

		// BUCLE FOR (sin anidamiento)
		System.out.println("Empieza bucle FOR:");
		for (x = 1; x <= 10; x++) {
			System.out.print(" " + x);
		}
		System.out.println();

		// BUCLE WHILE (sin anidamiento)
		System.out.println("Empieza bucle WHILE:");
		x = 1;
		while (x <= 10) {
			System.out.print(" " + x);
			x++;
		}
		System.out.println();

		// BUCLE DO WHILE (sin anidamiento)		
		System.out.println("Empieza bucle DO WHILE:");
		x = 1;
		do {
			System.out.print(" " + x);
			x++;
		}
		while (x <= 10);
		System.out.println();

	}
}
