class A{
    
public static void main(String[] args){
	
	try {
	
	try {
		throw generadorExcepciones(2);
	}
	catch(Exception3 eb) {System.out.println("Capturada Exception3");}
	catch(Exception2 ea) {System.out.println("Capturada Exception2");}
	//catch(Exception e) {System.out.println("Capturada Exception");}
	
	int SC = 42;
	
	} catch(Exception e) {System.out.println("Capturada Exception");} 
}
	
static Exception generadorExcepciones(int x){
	Exception e1 = new Exception();
	Exception2 e2 = new Exception2();
	Exception3 e3 = new Exception3();
	
	switch (x) {
		case 1: return e1; 
		case 2: return e2;
		case 3: return e3;		
	}
	return e1;
}
}

class Exception2 extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}

class Exception3 extends Exception2{

/**
 * 
 */
	private static final long serialVersionUID = 1L;
}