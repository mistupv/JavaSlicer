package ejemplos;

public class Bucles_2 {

	public static void main(String[] args) {
		// BUCLE WHILE anidado a otro WHILE
		System.out.println("Empieza bucle WHILE anidado a otro WHILE:");
		int x = 1;
		char y = 'a';
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
	}
}
