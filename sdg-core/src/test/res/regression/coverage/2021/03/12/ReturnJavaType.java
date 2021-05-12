class Test {

    Integer f() {
        return Integer.valueOf(10);
    }

    public static void main(String[] args) {
        System.out.println(new Test().f());
    }
}

