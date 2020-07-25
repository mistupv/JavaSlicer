public class ReturnTest {
	public static void main(String[] args) {
		int i = Integer.valueOf(args[0]);
		if (i == 0) {
			System.out.println("true");
			return;
		} else {
			System.out.println("false");
			return;
		}
	}
}