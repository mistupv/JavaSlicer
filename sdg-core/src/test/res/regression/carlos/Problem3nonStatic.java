class Problem3 {
    int x;

    public void main() throws Exception {
        x = 0;
        try {
            f();
        } catch (Exception e) {
            System.out.println("error");
        }
        x = 1;
        f();
    }

    public void f() throws Exception {
        if (x % 2 == 0)
            throw new Exception("error!");
        System.out.println("x = " + x);
    }
}
