package Optimitzador;

import java.util.ArrayList;

public class deadCodeFunction {
    public String name;
    public boolean found;
    public ArrayList<Integer> lines;

    public deadCodeFunction(String name, boolean found, ArrayList<Integer> lines) {
        this.name = name;
        this.found = found;
        this.lines = lines;
    }
}
