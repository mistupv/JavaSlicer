class A{

	int xA = 0;
	int vA = 1;

	A(int newx) {
		xA = newx;
	}

	int getx() {
		return xA;
	}

	void setx(int newx) {
		xA = newx;
	}

	int getv() {
		return vA;
	}

	void setv(int newv) {
		vA = newv;
	}

	public static void main(String[] args) {
		A a1 = new A(1);
		A a2 = new A(2);
		B b1 = new B(5.6);

		int y;
		y = a1.getx();

		int basura = 3;
		basura = y + a1.getx();

		a2.setx(6);

		b1.modificarxB(b1);
		int z = b1.getxB(); //y + a2.getx();
		System.out.println(z);
	}

}

class B extends A{
	int xB = 5;

	B(double valor){
		super((int) valor);
	}

	void modificarxB(B b){
		b.xB=10;
	}

	int getxB() {
		return xB;
	}
}
