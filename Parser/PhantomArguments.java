package Parser;

public class PhantomArguments {
    private int lineNumber;
    private String funcName;
    private int argumentCount;

    public PhantomArguments(int lineNumber, String funcName, int argumentCount) {
        this.lineNumber = lineNumber;
        this.funcName = funcName;
        this.argumentCount = argumentCount;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFuncName() {
        return funcName;
    }

    public int getArgumentCount() {
        return argumentCount;
    }
}
