class A{

int xA = 0;
    
A(int newx){xA=newx;}

int getx() {return xA;}
void setx(int newx) {xA=newx;}
void noModificaxA() {}
void siModificaxA(int v) {xA++;}
    
public static void main(String[] args){
    
	A a1 = new A(1);
  	A a2 = new A(2);
	B b1 = new B(5.6);
	 
    a1.noModificaxA(); 
    a2.siModificaxA(5);	
    b1.noModificaxA();
    b1.siModificaxA(5);	
        

    int z = a1.getx();
    System.out.println(z);
}

}

class B extends A{
	int xB = 5;
	
	B(double valor){
		super((int) valor);
		}
	
	void modificarxB(B b){
		b.setxB(10);
	}
	
	int getxB() {return xB;}
	void setxB(int valor) {xB=valor;}
	
	void noModificaxA() {System.out.println("useless");}
	void siModificaxA(int v) {super.siModificaxA(v);}
}