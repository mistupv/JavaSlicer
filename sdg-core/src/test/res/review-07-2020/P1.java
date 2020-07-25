// Problem: both 'equals' methods were included
// Solution
// 1. The output --> output (method_output -> method_call_return) was not classified as output.
// 2. The second pass of the slicing algorithm was misbehaving.
/**
 * class FiguresGroup.
 *
 * @author LTP
 * @version 2018-19
 */
class FiguresGroup {
    private static final int NUM_FIGURES = 10;
    private Figure[] figuresList = new Figure[NUM_FIGURES];
    private int numF = 0;

    public void add(Figure f) { figuresList[numF++] = f; }

    public String toString() {
        String s = "";
        for (int i = 0; i < numF; i++) {
            s += "\n" + figuresList[i];
        }
        return s;
    }
    private boolean found(Figure f) {
        for (int i = 0; i < numF; i++) {
            if (figuresList[i].equals(f)) return true;
        }
        return false;
    }
    private boolean included(FiguresGroup g) {
        for (int i = 0; i < g.numF; i++) {
            if (!found(g.figuresList[i])) return false;
        }
        return true;
    }
    public boolean equals(Object o) {
        if (!(o instanceof FiguresGroup)) return false;
        FiguresGroup g = (FiguresGroup) o;
        return this.included(g) && g.included(this);
    }

    public double area() {
        double a = 0.0;
        for (int i = 0; i < numF; i++) {
            a += figuresList[i].area();
        }
        return a;
    }

    public Figure greatestFigure() {
        if (numF == 0) return null;
        Figure f = figuresList[0];
        double a = figuresList[0].area();
        for (int i = 1; i < numF; i++) {
            double b = figuresList[i].area();
            if (a < b) {
                f = figuresList[i];
                a = b;
            }
        }
        return f; // SLICING CRITERION
    }
}

class Figure{
    int area() {return 5;}
}
