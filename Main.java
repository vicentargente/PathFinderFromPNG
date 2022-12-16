import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.PriorityQueue;

public class Main {
    private static final byte FREE_PATH = 0;
    private static final byte WALL = 1;
    private static final byte SOLVED_PATH = 2;
    private static final byte ORIGIN = 3;
    private static final byte DESTINATION = 4;
    private static final byte UNSOLVED_PATH = 5;

    public static void main(String[] args){

        if(args.length != 1){
            System.out.println("Ussage: java " + Main.class.getName() + " mazeImagePath");
            System.exit(0);
        }

        byte[][] maze = getMazeFromImage(PNGUtil.getImgPixels(args[0]));
        Coords[] startAndFinish = getMazeStartAndFinish(maze);
        if(startAndFinish == null){
            System.err.println("Could not find valid start and end point");
            System.exit(-1);
        }

        long tini, tfin;
        tini = System.nanoTime();
        boolean solved = aStar(maze, startAndFinish[0], startAndFinish[1]);
        tfin = System.nanoTime();
        System.out.println("A*: " + (tfin - tini)/1000000.0 + "ms");
        
        String solvedImageName = solved ? "Solved.png" : "NotSolved.png";
        PNGUtil.saveRgbPng(args[0].substring(0, args[0].lastIndexOf(".")) + solvedImageName, getImageFromMaze(maze));
    }

    private static boolean aStar(byte[][] matrix, Coords origin, Coords destination){
        HashMap<Coords, Integer> open = new HashMap<>(); //Key -> Coords, Value -> f(n) = g(n) + h(n)
        HashMap<Coords, Coords> close = new HashMap<>(); //Key -> Coords, Value -> Coordenadas de donde viene (asi podemos recorrer para conseguir el camino)
        
        close.put(origin, origin);
        addNeighbours(open, close, matrix, origin, destination, 1);
        if(open.size() == 0){
            return false;
        }
        Coords current = null;
        while(open.size() > 0){
            current = minElementInHashMap(open);

            close.put(current, current.getMin());

            if (current.equals(destination)){
                break;
            }
            addNeighbours(open, close, matrix, current, destination, current.getG() + 1);
        }

        boolean solved = open.size() > 0;

        byte pathNumber = solved ? SOLVED_PATH : UNSOLVED_PATH;

        Coords last = current;
        while(!last.equals(origin)){
            current = close.get(last);
            matrix[current.getRow()][current.getColumn()] = pathNumber;
            last = current;
        }
        matrix[origin.getRow()][origin.getColumn()] = ORIGIN;
        matrix[destination.getRow()][destination.getColumn()] = DESTINATION;
        return solved;
    }

    private static void addNeighbours(HashMap<Coords, Integer> open, HashMap<Coords, Coords> close, byte[][] matrix, Coords coords, Coords destination, int g){
        int row = coords.getRow(), column = coords.getColumn();
        Coords toAdd;
        Integer currentF;
        int f;

        if (row > 0 && matrix[row - 1][column] == 0) {
            toAdd = new Coords(row - 1, column, g);
            f = g + toAdd.manhattan(destination);
            currentF = open.get(toAdd);

            if (currentF == null) {
                if(close.get(toAdd) == null){
                    open.put(toAdd, f);
                    toAdd.setMin(coords);
                }
            } else {
                if (currentF > f) {
                    open.replace(toAdd, f);
                    toAdd.setMin(coords);
                }
            }
        }
        if (row < matrix.length - 1 && matrix[row + 1][column] == 0) {
            toAdd = new Coords(row + 1, column, g);
            f = g + toAdd.manhattan(destination);
            currentF = open.get(toAdd);
            if (currentF == null) {
                if(close.get(toAdd) == null){
                    open.put(toAdd, f);
                    toAdd.setMin(coords);
                }
            } else {
                if (currentF > f) {
                    open.replace(toAdd, f);
                    toAdd.setMin(coords);
                }
            }
        }
        if (column > 0 && matrix[row][column - 1] == 0) {
            toAdd = new Coords(row, column - 1, g);
            f = g + toAdd.manhattan(destination);
            currentF = open.get(toAdd);
            if (currentF == null) {
                if(close.get(toAdd) == null){
                    open.put(toAdd, f);
                    toAdd.setMin(coords);
                }
            } else {
                if (currentF > f) {
                    open.replace(toAdd, f);
                    toAdd.setMin(coords);
                }
            }
        }
        if (column < matrix[0].length - 1 && matrix[row][column + 1] == 0) {
            toAdd = new Coords(row, column + 1, g);
            f = g + toAdd.manhattan(destination);
            currentF = open.get(toAdd);
            if (currentF == null) {
                if(close.get(toAdd) == null){
                    open.put(toAdd, f);
                    toAdd.setMin(coords);
                }
            } else {
                if (currentF > f) {
                    open.replace(toAdd, f);
                    toAdd.setMin(coords);
                }
            }
        }

        open.remove(coords);
    }
    
    private static Coords minElementInHashMap(HashMap<Coords, Integer> map) {
        Integer[] minValue = new Integer[]{Integer.MAX_VALUE};
        Coords[] minNode = new Coords[]{null};
        map.forEach((k, v) -> {
            if(v < minValue[0]){
                minValue[0] = v;
                minNode[0] = k;
            }
        });
        return minNode[0];
    }


    private static byte[][] getMazeFromImage(byte[][][] img){
        byte[][] res = new byte[img.length][img[0].length];
        for(int i = 0; i < res.length; i++){
            for(int j = 0; j < res[i].length; j++){
                //Si encontramos 0 0 0 no hacemos nada porque la matriz ya vale 0
                if(img[i][j][0] == 0 && img[i][j][1] == 0 && img[i][j][2] == 0){ //Si es negro
                    res[i][j] = WALL; // Pared
                }
                else if (img[i][j][0] == 0 && img[i][j][1] == (byte) 0xFF && img[i][j][2] == 0) { // Si es verde
                    res[i][j] = ORIGIN; // Inicio
                }
                else if (img[i][j][0] == (byte) 0xFF && img[i][j][1] == 0 && img[i][j][2] == 0) { // Si es rojo
                    res[i][j] = DESTINATION; // Final
                }
                //Si es otro color lo ignoramos
            }
        }
        return res;
    }

    private static byte[][][] getImageFromMaze(byte[][] maze){
        byte[][][] res = new byte[maze.length][maze[0].length][3];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                if (maze[i][j] == FREE_PATH) {
                    res[i][j][0] = (byte) 0xFF;
                    res[i][j][1] = (byte) 0xFF;
                    res[i][j][2] = (byte) 0xFF;
                } else if (maze[i][j] == SOLVED_PATH) {
                    res[i][j][2] = (byte) 0xFF;
                } else if (maze[i][j] == UNSOLVED_PATH) {
                    res[i][j][0] = (byte) 0xFF;
                    res[i][j][2] = (byte) 0xFF;
                } else if (maze[i][j] == ORIGIN) {
                    res[i][j][1] = (byte) 0xFF;
                } else if (maze[i][j] == DESTINATION) {
                    res[i][j][0] = (byte) 0xFF;
                }
                //Si encontramos 1 no pintamos de negro porque la matriz ya vale 0 0 0
            }
        }
        return res;
    }

    private static Coords[] getMazeStartAndFinish(byte[][] maze){
        Coords[] res = new Coords[2];
        boolean canFinish = false;
        for(int i = 0; i < maze.length; i++){
            for(int j = 0; j < maze[i].length; j++){
                if(maze[i][j] == DESTINATION){
                    res[1] = new Coords(i, j);
                    maze[i][j] = 0;
                    if (canFinish) {
                        return res;
                    }
                    canFinish = true;
                } else if (maze[i][j] == ORIGIN) {
                    res[0] = new Coords(i, j);
                    maze[i][j] = 0;
                    if (canFinish) {
                        return res;
                    }
                    canFinish = true;
                }
            }
        }
        if(res[0] == null){
            int i, j = 0;
            boolean found = false;
            for(i = 0; i < maze.length; i++){
                for(j = 0; j < maze[i].length; j++){
                    if(maze[i][j] == FREE_PATH){
                        found = true;
                        break;
                    }
                }
                if(found){
                    break;
                }
            }
            if(i == maze.length && j == maze[0].length){
                return null;
            }
            res[0] = new Coords(i, j);
        }
        if(res[1] == null){
            int i, j = 0;
            boolean found = false;
            for (i = maze.length - 1; i >= 0; i--) {
                for (j = maze[i].length - 1; j >= 0; j--) {
                    if (maze[i][j] == 0) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    break;
                }
            }
            if(i == -1 && j == -1){
                return null;
            }
            res[1] = new Coords(i, j);
        }
        return res;
    }

    /*
    private static void printMatrix(byte[][] matrix){
        String res = "";
        for(int i = 0; i < matrix.length; i++){
            for(int j = 0; j < matrix[0].length; j++){
                res += matrix[i][j] + " ";
            }
            res += "\n";
        }
        System.out.println(res);
    }

    
    private static void printHashMap(HashMap<Coords, Integer> map){
        Set<Coords> aux = map.keySet();
        Coords[] keys = aux.toArray(new Coords[aux.size()]);
        for (int i = 0; i < keys.length; i++) {
            System.out.println("K: (" + keys[i].getRow() + ", " + keys[i].getColumn() + "), V: " + map.get(keys[i]));
        }
    }

    private static void printHashMap2(HashMap<Coords, Coords> map) {
        Set<Coords> aux = map.keySet();
        Coords[] keys = aux.toArray(new Coords[aux.size()]);
        Coords v;
        for (int i = 0; i < keys.length; i++) {
            v = map.get(keys[i]);
            System.out.println("K: (" + keys[i].getRow() + ", " + keys[i].getColumn() + "), V: (" + v.getRow() + ", " + v.getColumn() + ")");
        }
    }
    */
    
}