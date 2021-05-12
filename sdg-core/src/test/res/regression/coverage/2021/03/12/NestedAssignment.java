class Test {

    int a = 10;

    public static void main(String[] args) {
        Test t1;
        Test t2;
        t1 = t2 = new Test();
        System.out.println(t1.a + t2.a);
    }
}

