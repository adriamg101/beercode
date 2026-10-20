package Parser;

import Semmantic.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ParserErrorHandler {
    private int currLine;
    private String currSymbol;
    private int errorCount;

    private ArrayList<PhantomArguments> phantoms;

    public ParserErrorHandler() {
        this.currLine = 1;
        this.currSymbol = "";
        this.errorCount = 0;
        this.phantoms = new ArrayList<>();
    }

    public void nextLine() {
        this.currLine++;
    }

    public void setSymbol(String symbol) {
        this.currSymbol = symbol;
    }

    public void reportLexicalError() {
        System.out.println("\u001B[33mERROR! LEXIC ERROR! (unrecognized symbol!)\n\tInfo: Near \""+currSymbol+"\" @ line"+currLine+"\u001B[0m");
        this.errorCount++;
    }

    public void reportIllegalSymbol(String varType) {
        System.out.println("\u001B[33mERROR! Illegal Symbol! (Used a name not allowed or already in use!)\n\tInfo: Near \""+currSymbol+"\" @ line"+currLine+" (illegal "+varType+" name)\u001B[0m");
        this.errorCount++;
    }

    public void reportSyntaxError(String expected) {
        System.out.println("\u001B[33mERROR! Syntax ERROR! (missing word, bad order, ...!)\n\tInfo: Near \""+currSymbol+"\" @ line"+currLine+" - Expected a "+expected+"\u001B[0m");
        this.errorCount++;
    }

    public void reportUndefinedVariable(String id) {
        System.out.println("\u001B[33mERROR! Semantic ERROR! (Undefined variable or used function name where var name should be used!)\n\tInfo: Symbol \""+id+"\" @ line"+currLine+"\u001B[0m");
        this.errorCount++;
    }

    public void reportUndefinedFunction(String list) {
        System.out.println("\u001B[33mERROR! Semantic ERROR! (Undefined function(s)!)\n\tInfo: The following functions haven't been defined \""+list+"\", yet they are called.\u001B[0m");
        this.errorCount++;
    }
    public void noReturnException(String func) {
        System.out.println("\u001B[33mERROR! Semantic ERROR! (A return statement is needed and none was found!)\n\tFunction: "+func+"\u001B[0m");
        this.errorCount++;
    }

    public void reportTypeMismatch(String id) {
        System.out.println("\u001B[33mERROR! Semantic ERROR! (Type mismatch! [check returns, operations, arguments, ...])\n\tInfo: Symbol \""+id+"\" @ line"+currLine+"\u001B[0m");
        this.errorCount++;
    }

    public void reportArgumentCountMismatch(String id, int line) {
        if (line == 0) System.out.println("\u001B[33mERROR! Semantic ERROR! (Argument over/underflow)\n\tInfo: Called function \""+id+"\" with incorrect number of arguments @ line"+currLine+"\u001B[0m");
        else System.out.println("\u001B[33mERROR! Semantic ERROR! (Argument over/underflow)\n\tInfo: Called function \""+id+"\" with incorrect number of arguments @ line"+line+"\u001B[0m");
        this.errorCount++;
    }

    public void reportIndexMismatch(String id) {
        System.out.println("\u001B[33mERROR! Semantic ERROR! (Index mismatch! [Tried to index an array by something not an Integer])\n\tInfo: Symbol \""+id+"\" @ line"+currLine+"\u001B[0m");
        this.errorCount++;
    }

    public void reportIncorrectCLSymbol(String id) {
        System.out.println("\u001B[33mERROR! Semantic ERROR! Closing line symbol not adecuate! \n\tExpected symbol: \""+id+"\" @ line"+currLine+"\u001B[0m");
        this.errorCount++;
    }

    public void recordPhantomArguments(String funcName, int argN) {
        this.phantoms.add(new PhantomArguments(currLine, funcName, argN));
    }

    public void checkPhantoms(ArrayList<String> pendingList, HashMap<Integer, List<Symbol>> symbolTable) {
        for (int i = 0; i < phantoms.size(); i++) {
            if (!pendingList.contains(phantoms.get(i).getFuncName())) {
                for (int j = 0; j < symbolTable.get(0).size(); j++) {
                    if (symbolTable.get(0).get(j).getName().equals(phantoms.get(i).getFuncName())) {
                        if (symbolTable.get(0).get(j).getArguments().size() != phantoms.get(i).getArgumentCount()) {
                            reportArgumentCountMismatch(phantoms.get(i).getFuncName(), phantoms.get(i).getLineNumber());
                        }
                    }
                }
            }
        }
    }

    public int getErrorCount(){
        return this.errorCount;
    }
}
