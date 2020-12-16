
class A{

int x = 0;
    
A(int x){this.x=x;}

int getx() {return this.x;}
void setx(int x) {this.x=x;}
    
public static void main(int[] args){
    
	A a = new A(1);
	
	int y;
    y=a.getx();
    int basura = 3;
    basura = y + a.getx();
    int z = y;
}

}