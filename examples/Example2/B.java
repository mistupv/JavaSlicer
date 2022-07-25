public class B extends A {
	int b = 5;
	public B(double val){
		super((int) val);
	}
	public void updateB(B b){
		b.setB(10);
	}
	public int getB() { return b; }
	public void setB(int val) { b = val; }
	public void printA() { 
		System.out.print("Useless"); 
	}
	public void updateA(int v) { 
		super.updateA(v); 
		a += v;
	}
}
