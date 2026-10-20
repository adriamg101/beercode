package Mips;

public class StackSlot {
    int offset;
    int size;
    boolean isFree;
    String varName;

    StackSlot(int offset, int size, String varName) {
        this.offset = offset;
        this.size = size;
        this.isFree = false;
        this.varName = varName;
    }
}

