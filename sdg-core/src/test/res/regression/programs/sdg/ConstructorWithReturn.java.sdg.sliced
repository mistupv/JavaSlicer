class Test {
    int a;

    Test(int a, boolean b, int c) {
        if (b) {
            this.a = a * c / (a + c);
            return;
        }
        this.a = 10;
    }

    public static void main(String[] args) {
        Test t = new Test(15, true, 10);
        int a = t.a;
        System.out.println(a);
    }
}