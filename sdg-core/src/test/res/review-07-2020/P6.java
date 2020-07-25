
public class Bucles {

    public static void main(String[] args){

        int x=2;
        try {

            for(int i = 0; i <= 12; i++)
            {
                System.out.print("12 * "+ i + " = " + 12 * i + "\n");
                metodoGeneradorExcepciones(x);
            }
        }
        catch (ExceptionB a)
        {
            System.out.println("Se lanza ExceptionB");
        }
        catch (ExceptionA a)
        {
            System.out.println("Se lanza ExceptionB");
        }
        catch (Exception a)
        {
            System.out.println("Se lanza Exception");
        }
    }

    static void metodoGeneradorExcepciones(int x) throws Exception {
        if (x==0) throw new ExceptionA();
        if (x==1) throw new ExceptionB();
        if (x==2) throw new Exception();
    }

}

class ExceptionA extends Exception{

    /**
     *
     */
    private static final long serialVersionUID = 1L;
}

class ExceptionB extends ExceptionA{

    /**
     *
     */
    private static final long serialVersionUID = 1L;
}
