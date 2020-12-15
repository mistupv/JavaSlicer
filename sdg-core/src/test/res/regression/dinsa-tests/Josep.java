
public class Josep {

	public static void main(String[] args) {
		int suma =0;
		suma += factorial(5);
		suma += factorial2(5);
	     System.out.print(suma);   // SLICING CRITERION
	}
	
	static int factorial(int x)
	{  
	     int contador = 1;
	     int fact = 1;
	     while (contador<x)
	     {
	          contador++; 
	          fact = fact * contador;
	      }
	     System.out.print(fact);
	     return fact;
	}    

	
	static int factorial2(int x)
	{  
	     int contador = 1;
	     int fact = 1;
	     if (contador<x)
	     {
	          int[] result = nuevoMetodo(fact, contador, x); 
	          fact = result[0];
	          contador = result[1];
	      }    

	     System.out.print(fact);
	     return fact;
	}    
	
	static int[] nuevoMetodo(int fact, int contador, int x)
	{
	      contador++; 
	      fact = fact * contador;    
	      if (contador<x)
	          return nuevoMetodo(fact, contador, x);
	      return new int[] {fact, contador};
	}
	
}
