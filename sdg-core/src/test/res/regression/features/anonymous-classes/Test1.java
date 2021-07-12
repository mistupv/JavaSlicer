class Test1 {
    public static void main(String[] args) {
        A a = new A() {
            @Override
            int compute() {
                return 2;
            }
        };
        int b = a.compute();
        System.out.println(b);
    }
}

class A {
    int compute() {
        return 0;
    }
}