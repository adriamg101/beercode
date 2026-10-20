package Semmantic;

import Exceptions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class SemmanticChecker {
    private boolean expectingID = false;
    private boolean expectingFunc = false;
    private boolean expectingReturn = false;
    private String lastFunction = "";
    private String currentFunction = "";
    private boolean needsReturn = false;
    private int expectingArg = -1;
    private boolean expectingIndex = false;
    private boolean argsStart = false;
    private boolean phantomFunc = false;
    private int phantomArgs = 0;
    private boolean expectingOp = false;
    private boolean expectingGracias = false;
    private boolean expectingQue = false;
    private boolean expectingLoSiento = false;
    private boolean returnFound=false;
    private SymbolTable table;
    public int contCB=0;

    private Set<String> argumentTypes = Set.of("ID", "CTE_ENTER", "CTE_DECIMAL", "CTE_CHARACTER");   //Argument can be ID (opTypes), enter, decimal or character
    private ArrayList<String> indexTypes = new ArrayList<>(Arrays.asList("INT"));
    private ArrayList<String> opTypes = new ArrayList<>(Arrays.asList("INT", "CHAR", "FLOAT", "DOUBLE", "LONG"));

    public SemmanticChecker (SymbolTable t) {
        this.table = t;
    }

    //Main method
    public void checkNextToken(String tok, String name) throws UndefinedSymbolException, UndefinedFunctionException, TypeMismatchException, ArgumentCountException, RecordPhantomArgumentsException, IndexMismatchException, incorrectCloseLineSymbolException, noReturnException {
        if(tok.equals("OB") && !name.equals("")) {
            contCB++;
        }
        if(tok.equals("CB") && !name.equals("")) {
            contCB--;
        }
        if (tok.equals("<ASSIGNATION>")) {
            expectingID = true;
            expectingGracias=true;
        } else {
            if (tok.equals("<DECLARATION>")) {
                expectingGracias = true;
            } else {
                if (tok.equals("<RETURN>") && contCB == 1) {
                    expectingID = true;
                    expectingReturn = true;
                    returnFound=true;
                } else {
                    if (tok.equals("FUNC") && !name.equals("")) {
                        needsReturn = true;
                    } else {
                        if (tok.equals("FUNC_CALL")) {
                            expectingFunc = true;
                            expectingGracias = true;
                        } else {
                            if ((tok.equals("ADD") || tok.equals("MULTIPLY")) && !name.equals("")) {
                                expectingLoSiento = true;
                            } else {
                                if ((tok.equals("SUBTRACT") || tok.equals("DIVIDE")) && !name.equals("")) {
                                    expectingQue = true;
                                } else {
                                    if (tok.equals("WITH") && !name.equals("")) {
                                        argsStart = true;
                                    } else {
                                        if (tok.equals("OC") && !name.equals("")) {
                                            expectingIndex = true;
                                        } else {
                                            if (tok.equals("<OPERATION>")) {
                                                expectingID = true;
                                                expectingOp = true;
                                            } else {
                                                if (contCB == 0 && tok.equals("CB") && !name.equals("") && needsReturn) {
                                                    checkIsReturn();
                                                } else {
                                                    if (tok.equals("CL") && !name.equals("") || tok.equals("OB") && !name.equals("")) {
                                                        expectingID = false;
                                                        expectingFunc = false;
                                                        expectingReturn = false;
                                                        manageArgumentCount(false);
                                                        expectingArg = -1;
                                                        argsStart = false;
                                                        expectingIndex = false;
                                                        expectingOp = false;
                                                        checkCl(name);
                                                        managePhantomArguments();
                                                    } else {
                                                        if (tok.equals("ID") && (expectingID || expectingFunc) && !name.equals(""))
                                                            checkIDExists(name);
                                                        if (tok.equals("ID") && (expectingReturn) && !name.equals(""))
                                                            checkReturnType(name);
                                                        if (argumentTypes.contains(tok) && (expectingArg != -1 && argsStart) && !name.equals(""))
                                                            manageArgumentCount(true);
                                                        if (argumentTypes.contains(tok) && (phantomFunc && argsStart) && !name.equals(""))
                                                            phantomArgs++;
                                                        if (tok.equals("ID") && (expectingIndex) && !name.equals(""))
                                                            checkIndexType(name);
                                                        if (tok.equals("ID") && (expectingOp) && !name.equals(""))
                                                            checkOperationalType(name);
                                                        if (tok.equals("ID") && needsReturn && !name.equals("") && currentFunction.equals(""))
                                                            currentFunction = name;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkIsReturn() throws noReturnException{
        if(!returnFound && !table.funcVoid(currentFunction)) {
            returnFound=false;
            needsReturn=false;
            String name = currentFunction;
            currentFunction = "";
            throw new noReturnException(name);
        }
        returnFound=false;
        needsReturn=false;
        currentFunction = "";
    }
    private void checkCl(String name) throws incorrectCloseLineSymbolException {
        if (expectingGracias && !name.equals("gracias")) {
            expectingGracias = false;
            throw new incorrectCloseLineSymbolException("gracias");
        }else if (expectingQue && !name.equals("que?")) {
            expectingQue = false;
            throw new incorrectCloseLineSymbolException("que?");
        }else if (expectingLoSiento && !name.equals("lo siento")) {
            expectingLoSiento = false;
            throw new incorrectCloseLineSymbolException("lo siento");
        }
        expectingGracias=false;
        expectingQue=false;
        expectingLoSiento=false;
    }

    //Check if valiable is declared
    private void checkIDExists(String varName) throws UndefinedSymbolException {
        if (!this.table.isSymbolDefined(varName, expectingFunc, expectingID)) {
            if (!expectingFunc) {
                throw new UndefinedSymbolException(varName);
            }
            else {
                //If is expecting a function name that hasn't been defined yet... we'll hold on to this until the end!
                lastFunction = varName;
                phantomFunc = true;
                expectingFunc = false;
                expectingID = true;
                this.table.addExpectation(varName);
            }
        } else {
            if (expectingFunc) {
                lastFunction = varName;
                expectingArg = this.table.argumentsFor(lastFunction);
                expectingFunc = false;
                expectingID = true;
            }
        }
    }

    //Check if returned variable matches function type
    private void checkReturnType(String varName) throws TypeMismatchException {
        expectingReturn = false;
        if (!this.table.isReturnTypeOK(varName)) {
            throw new TypeMismatchException(varName);
        }
    }

    private void manageArgumentCount(boolean newVar) throws ArgumentCountException {
        if ((expectingArg == 0 && newVar) || (expectingArg > 0 && !newVar)) {
            expectingArg = -1;
            argsStart = false;
            throw new ArgumentCountException(lastFunction);
        }
        if (expectingArg > 0 && newVar) expectingArg--;
    }

    private void managePhantomArguments() throws RecordPhantomArgumentsException {
        int n = phantomArgs;
        boolean b = phantomFunc;
        phantomFunc = false;
        phantomArgs = 0;
        if (b) throw new RecordPhantomArgumentsException(lastFunction, n);
    }

    private void checkIndexType(String varName) throws IndexMismatchException {
        expectingIndex = false;
        if (!this.table.isIndexTypeOK(varName, indexTypes)) {
            throw new IndexMismatchException(varName);
        }
    }

    private void checkOperationalType(String varName) throws TypeMismatchException {
        if (!this.table.isIndexTypeOK(varName, opTypes)) {
            throw new TypeMismatchException(varName);
        }
    }

    public void checkPendingSymbols() throws UndefinedFunctionException {
        if (!this.table.checkPending()) {
            throw new UndefinedFunctionException(table.getPending());
        }
    }
}
