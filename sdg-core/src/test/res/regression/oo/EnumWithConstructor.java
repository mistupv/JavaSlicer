public class EnumWithConstructor {
    public static void main(String[] args) {
        String res = "";
        res += Dias.LUNES.getId();
        res += Dias.DOMINGO.getId();
        System.out.println(res);
    }
}

enum Dias {
    LUNES,
    MARTES,
    MIERCOLES,
    JUEVES,
    VIERNES,
    SABADO,
    DOMINGO;

    private char id = name().charAt(0);

    public char getId() {
        return id;
    }
}