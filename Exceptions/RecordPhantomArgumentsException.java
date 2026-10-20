package Exceptions;

public class RecordPhantomArgumentsException extends Exception {
    public String funcName;
    public int argN;
    public RecordPhantomArgumentsException(String funcName, int argN) {
        super("");
        this.funcName = funcName;
        this.argN = argN;
    }
}
