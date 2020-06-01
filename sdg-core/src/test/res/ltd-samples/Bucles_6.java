package ejemplos;

public class Bucles_6 {

	public static void main(String[] args) {
		// BUCLE WHILE (sin anidamiento)
		System.out.println("Empieza bucle WHILE:");
		int x = 1;
		while (x <= 10) {
			System.out.print(" " + x);
			x++;
			while (x <= 10) {
				System.out.print(" " + x);
				x++;
			}
		}
		while (x <= 10) {
			System.out.print(" " + x);
			x++;
		}
		System.out.println();
	}
}
