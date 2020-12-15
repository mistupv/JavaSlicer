class A {

    int xA = 0;

    int getx() {
        return xA;
    }

    void setx(int newx) {
        xA = newx;
    }

    void noModificaxA() {
    }

    void siModificaxA(int v) {
        xA++;
    }

    public static void main(String[] args) {

        A a1 = new A();

        a1.setx(5);
        a1.noModificaxA();
        a1.siModificaxA(5);

        int z = a1.getx();
        System.out.println(z);
    }
}
