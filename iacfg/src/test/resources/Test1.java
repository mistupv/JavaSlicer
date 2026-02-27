public class Test1 {
    public static void main(String[] args) {
        int numero = 7;

        numero = numero * 2;

        if (numero > 10) {
            System.out.println("El número es mayor que 10");
        } else {
            System.out.println("El número es 10 o menor");
        }

        switch (numero) {
            case 10:
                System.out.println("Valor exacto: 10");
                break;
            case 14:
                System.out.println("Valor exacto: 14");
                break;
            default:
                System.out.println("Otro valor");
        }

        for (int i = 0; i < 3; i++) {
            System.out.println("Iteración #" + i);
        }

        int contador = 0;
        while (contador < 2) {
            System.out.println("Contando: " + contador);
            contador++;
        }

        int j = 0;
        do {
            System.out.println("Ejecutando al menos una vez: " + j);
            j++;
        } while (j < 1);

        for (int k = 0; k < 5; k++) {
            if (k == 2) continue;
            if (k == 4) break;
            System.out.println("k = " + k);
        }

        ;
        {
            int temp = 5;
            System.out.println("Bloque local: " + temp);
        }
        return;
    }
}
