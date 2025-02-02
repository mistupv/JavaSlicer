package es.upv.mist.slicing.tests;

public class Test_Entrevista {

	public static void main(String[] args)
	{
		int x = 1;

		while (x > 1) {
			if (x > 10) {
				x++;
			} else {
				while (x > 1) {
					if (x > 10) {
						x++;
					} else {
						if (x > 10) {
							x++;
						} else {
							x--;
						}
					}
				}
			}
		}
	}
}
