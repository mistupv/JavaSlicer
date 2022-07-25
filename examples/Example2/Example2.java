public class Example2 {
	public static void main(String[] args){
		A a1 = new A(1);
		B b1 = new B(5.6);
		a1.printA(); 
		b1.printA();
		b1.updateA(5);	
		int z = b1.getA(); 
		System.out.print(z);
	}
}

