package ejemplos;

public class Bucles_4 {

	public static void main(String[] args) {
		int x = 1;

		//Bucle 1: Contador
		while (x < 10) {
			System.out.println(x);
			x++;
		}

		//Bucle 2: Sumatorio
		int suma = 0;
		int y = 1;
		while (y < 10) {
			suma += y;
			y++;
		}
		System.out.println(suma);

		//Bucle 3: Sumatorio
		int sumatorio = 0;
		int min = 10;
		int max = 100;
		for (int num = min; num <= max; num++) {
			sumatorio += num;
		}
		System.out.println(sumatorio);

		int count = 0;
		while (count < 10)
			count++;
		System.out.println(count);
	}
}