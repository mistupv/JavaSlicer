package es.upv.mist.slicing.tests;

public class Test {

	public static void main(String[] args)
	{
		int x = 1;
		boolean a = true;

		while (x < 100) {
			if (x < 50) {
				System.out.println(x);
				x = incrementar(x);
			} else {
				System.out.println(x);
				x = incrementarBucle(x);
			}
		}
	}

	private static int incrementar(int a){
		return a+1;
	}

	private static int incrementarBucle(int a){
		if(a>0){
			int x = 0;
			for(int i = 0; i<a; i++){
				x = x + i;
			}
			return x;
		} else {
			return a;
		}
	}
}
