package tfm.programs.cfg;

public class Eval_3 {

	public static void main(String[] args)
	{		
		int x=1;
		while (x>0)
		{
			if (x<=1)
			{
				x--;
			}
			else if (x<=2)
			{
				if (x<=3)
				{
					x++;
				}
				else
				{
					x--;
					while (x>1)
					{
						x--;
					}
				}	
			}
		}
	}
}
