package mytest;

public class BasicSwitch {
	public static void main(String[] args) {
		int x = Integer.valueOf(args[0]);
		int y = -1;
		switch (x) {
			case 1:
				y = 10;
				break;
			case 2:
				y = 20;
				break;
			case 3:
				y = 30;
				break;
			case 4:
			case 5:
				y = 100;
				break;
			case 6:
				System.err.println("Error");
			case 10:
				y = 0;
		}
		System.out.println(y);
	}
}
