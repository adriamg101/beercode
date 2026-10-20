package Mips;

public class MMUTableNode {
    private String varName;
    private String reg;
    private String mem;
    private int evictValue;

    public MMUTableNode(String varName, String reg, String mem, int evictValue) {
        this.varName = varName;
        this.reg = reg;
        this.mem = mem;
        this.evictValue = evictValue;   //This will be current line. If LRU value will be updated. Evicted by lowest value.
    }

    public String getVarName() {
        return varName;
    }

    public String getReg() {
        return reg;
    }

    public String getMem() {
        return mem;
    }

    public int getEvictValue() {
        return evictValue;
    }

    public void setReg(String reg) {
        this.reg = reg;
    }

    public void setMem(String mem) {
        this.mem = mem;
    }

    public void setEvictValue(int evictValue) {
        this.evictValue = evictValue;
    }

    public void setVarName(String s) {
        this.varName = s;
    }
}
