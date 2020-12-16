package mytest;

public class BasicSwitchNoBreak {
	public static void main(String[] args) {
		String res = "";
		switch (args[0]) {
			case "a":
				res = "abc";
			case "b":
				res = "bac";
			case "c":
				res = "cab";
		}
		System.out.println(res);
	}
}
