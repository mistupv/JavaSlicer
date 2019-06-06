package tfm.programs.cfg;

import tfm.utils.Logger;

public class Eval_1 {

	public static void main(String[] args)
	{	
		int x=1;
		while (x<=10)
		{
			x++;
			while (x<=10)
			{
				if (x>0)
				{
				}
				else
					x++;
			}
		}
		do {
			x--;

			if (x == 1) {
				x = 2;
			} else {
				x = 4;
			}
		} while (x < 1);

		for (int z = 1; z < x; z++, x--) {
			int k = z + x;
			System.out.println(k);
		}

		int[] numbers = { 1, 2, 3, 4, 5 };

		for (int number : numbers) {
			System.out.println(number);
		}

		switch (x) {
			case 1:
				int z = 3;
				x = 3;
				break;
			case 2:
				while (x < 9) {
					x++;
					break;
				}
				x = 4;
				break;
			case 3:
				x = 5;
				break;
			default:
				x = 5;
				break;
		}

		Logger.log(x);
	}
}
