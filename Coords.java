public class Coords implements Comparable {
    private int row;
    private int column;
    private int g; //g(n) -> Coste para llegar a este nodo
    private Coords min; //Nodo que ha hecho que se llegue a este con coste minimo

    public Coords(int row, int column){
        this.row = row;
        this.column = column;
        this.g = 0;
    }

    public Coords(int row, int column, int g){
        this.row = row;
        this.column = column;
        this.g = g;
    }

    public int getRow(){
        return this.row;
    }

    public int getColumn() {
        return this.column;
    }

    public int getG(){
        return this.g;
    }

    public void setG(int g){
        this.g = g;
    }

    public Coords getMin(){
        return this.min;
    }

    public void setMin(Coords min){
        this.min = min;
    }

    public int compareTo(Object o){
        return myAbs(this.row - ((Coords) o).row) + myAbs(this.column - ((Coords) o).column);
    }

    public int manhattan(Coords coords){
        return myAbs(this.row - coords.row) + myAbs(this.column - coords.column);
    }

    private static int myAbs(int number) {
        return number >= 0 ? number : -number;
    }

    public boolean equals(Object o){
        return o instanceof Coords && this.row == ((Coords) o).row && this.column == ((Coords) o).column;
    }

    public int hashCode(){
        //return (this.row + "." + this.column).hashCode();
        return (this.row << 16) | this.column;
    }
}