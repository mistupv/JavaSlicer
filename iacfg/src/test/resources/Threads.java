public class Threads {
    public static int a;

    public static void main(String[] args) {
        a = 1;
        f(2);
        Thread t1 = new Thread(new Task1()).start();
        Thread t2 = new Thread(new Task2()).start();
        t1.join();
        t2.join();
    }

    public static void f(int x) {
        a = x;
    }
}

class Task1 implements Runnable {
    @Override
    public void run() {
        Threads.f(3);
        Threads.f(4);
        System.out.println(Threads.a);
    }
}

class Task2 implements Runnable {
    @Override
    public void run() {
        Threads.f(5);
    }
}