// Problem: the declaration 'int x;' was not included in the slice
// Solution: declarations were never considered, there is now a naive approach
// which finds the previous declaration for each definition (if there is no declaration in the same node).
// This solution does not take into account the concept of "scopes", for variables declared within a 'for'.
public class Bucles {
	public static void main(String[] args){
		int x;
		for(int i = 0; i <= 12; i++)
			System.out.print("12 * "+ i + " = " + 12 * i + "\n");
        int i = 8;
        x = 5+i;
	}
}
