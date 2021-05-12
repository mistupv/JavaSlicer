class Test {

    int a;

    static void f(Test t) {
        t.a = 10;
    }

    public static void main(String[] args) {
        Test t1 = new Test();
        Test t2 = new Test();
        f(true ? t1 : t2);
        System.out.println("t1: " + t1.a + ", t2: " + t2.a);
    }
}

