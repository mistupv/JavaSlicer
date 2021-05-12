class Test {

    public static void main(String[] args) {
        try {
            if (true)
                throw new RuntimeException();
            throw new InterruptedException();
        } catch (RuntimeException | InterruptedException e) {
            System.out.println("ok");
        }
    }
}
