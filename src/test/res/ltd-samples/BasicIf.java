public class BasicIf {
	public static void main(String[] args) {
		boolean isMoving = false;
		int currentSpeed = 10;
		if (isMoving) {
			currentSpeed--;
		} else {
			System.err.println("The bicycle has already stopped!");
		}
	}
}