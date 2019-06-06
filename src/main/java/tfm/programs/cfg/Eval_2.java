package tfm.programs.cfg;

public class Eval_2 {

	public static void main(String[] args)
	{		
		int x=1;
		if (x<=1)
		{
			if (x<=2)
			{
				if (x<=3)
				{
					x++;
				}
				else x--;
			}
			else x--;
		}
		System.out.println(x);	
	}
}
