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

// 1. :D Definir this como -output- al final de los constructores.
// 2. Hay que copiar el arbol al usar un Movable para generar un nodo.
// 3. Hay que utilizar el arbol para generar el arbol de nodos correspondiente
// 4. En las llamadas a super que se resuelven a Object.super(), hay que definir 'this'.
// Por tanto todos los constructores empezaran [in]directamente por una definciion de this,
// y 'this' nunca se buscara en Interprocedural*Finder


// PARA RESOLVER problemas con this
/*
2. Cada vez que se define/usa un miembro, se realiza la misma accion para todos
los padres en el arbol (use(a.x) es tambien use(a)): podemos utilizar el arbol de ayer
3. siempre conectemos una actual/formal a un uso/def tenemos que copiar el arbol
(en actual cambiando el nombre de la raiz -- cambiar raiz y todos sus arcos).
 */
/*
Interprocedural*Finder: cuando se define this en un constructor hay qeu pasarlo de vuelta
a) es una creacion de obj normal: se traduce como retorno de la llamada.
b) es una creacion de obj con super()/this(): se traduce a 'this': se copia el arbol
 */
/*
cuando super() defina x, y, z, la salida DEF(this) que contendra las variables definidas.
buscara un DEC(this) y lo encontrara justo antes (INTRA)
el DEF(this) servira como elemento INTRA para enlazar el resto de statements del constr.
 */

/*
Preservar el orden de acciones al usar arboles:
Cuando se encuentra una accion repetida (e.g. USE(a.x), DEF(a.x), **USE(a.x)**),
tenemos que quitar del Factory la accion vieja (el primer USE(a.x), que sea un USE(a)
y contendra en su arbol (a.x)).

Ejemplo:
print(a.x++ + a.x); USE(a.x), DEF(a.x), USE(a.x)
Y despues de la visita se tratan las definiciones, pasando a ser:
USE(a.x), USE(a), DEF(a.x), DEF(a), USE(a.x), USE(a)
Modificamos el Interprocedural*Finder para solo tratar los elementos raiz.
Incluso podemos incluir una referencia al hijo en el raiz (USE(a) contiene USE(a.x))

Problema:
print(a.y++ + a.x + a.x++):
 */