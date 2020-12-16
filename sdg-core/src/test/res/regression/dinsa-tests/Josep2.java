
public class Josep2 {

	public static void main(String[] args) {
		int suma =0;
		suma += factorial(5);
		
		Numeros num = new GrandesNumeros(7);
		
		suma += num.random(); 
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
}

class Numeros{
	
	int haceFalta = 1;
	int noHaceFalta = 1;
	
	int random(){
		return haceFalta;
	}
	
}

class GrandesNumeros extends Numeros {
	
	GrandesNumeros(double x){
		haceFalta = 0;
		noHaceFalta = 0;
	}
	
	int random(){
		return haceFalta;
	}
	
}