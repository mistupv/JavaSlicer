// Problem: there is no interprocedural-output edge between normal exit and normal return, exception exit and exception return!
// Modified ESSDG#build to modify MethodCallReplacerVisitor (created a sub-class).
public class Bucles {

    static int x=0;
    
	public static void main(String[] args){

	try {
		metodoGeneradorExcepciones();
	}
    catch (ExceptionB a)
    {
    	System.out.println("Se lanza ExceptionB");
    	main(args);
	}
    catch (ExceptionA a)
    {
    	System.out.println("Se lanza ExceptionA");
    	main(args);
	}
    catch (Exception a)
    {
    	System.out.println("Se lanza Exception");
    	main(args);
	}
	
	System.out.println("No se lanza ninguna excepcion");
	
	}

	static void metodoGeneradorExcepciones() throws Exception {
		if (x==0) {
			x=1;
			throw new ExceptionA();
		}
		if (x==1) {
			x=2; 
			throw new ExceptionB();
		}
		if (x==2) {
			x=3;
			throw new Exception();
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
