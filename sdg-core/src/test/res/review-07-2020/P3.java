// Problem: In the third 'catch' there are no ways to traverse backwards, because the only arc is a CC1.
// The exception sources "should" be connected to the catch so that they can be included. ???
// Solution: not solved
public class Bucles {

	public static void main(String[] args){
	    int x=2;
		try {
			for(int i = 0; i <= 12; i++)
				System.out.print("12 * "+ i + " = " + 12 * i + "\n");
			if (x==0)
				throw new ExceptionA();
			if (x==1)
				throw new ExceptionB();
			if (x==2)
				throw new Exception();
		}
    	catch (ExceptionB a)
    	{
    		System.out.println("Se lanza ExceptionB");
		}
		catch (ExceptionA a)
		{
			System.out.println("Se lanza ExceptionA");
		}
		catch (Exception a)
		{
			System.out.println("Se lanza Exception");
		}
	}
}
	
	class ExceptionA extends Exception{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}

	class ExceptionB extends ExceptionA{

	/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
	}
