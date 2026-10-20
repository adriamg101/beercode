package Semmantic;
import Exceptions.IllegalSymbolException;
import Lexer.Token;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SymbolTable {
    public final HashMap<Integer, List<Symbol>> symbolTable;
    private int currentScope;
    private int nextScope;

    final static String birraNamePath="Semmantic/MarcasBirras.json";
    final static String nombreBaresPath="Semmantic/NombreBares.json";

    //SYMBOL TABLE NAVIGATION (creation, etc)
    private Boolean expectingFuncId=false;
    private String symbolName;
    private Boolean expectingReturnType=false;
    private DataType symbolType;
    private String currentArgType;
    private int currentArraySize;
    private boolean expectingVarId=false;
    private boolean expectingA=false;
    private boolean expectingVarType=false;
    private boolean expectingOf=false;
    private boolean expectingOpenParen=false;
    private boolean expectingOpenBrace = false;
    private boolean expectingArgType = false;
    private List<Argument> arguments;
    private boolean expectingArgName=false;
    private boolean expectingCommaOrCP=false;
    private boolean expectingSizeArrya=false;
    public int contCB=0;
    private ArrayList<String> expectingFunctions;

    public SymbolTable() {
        this.symbolTable = new HashMap<>();
        this.symbolTable.put(0, new ArrayList<>()); // PONEMOS SCOPE GLOBAL (0)
        this.currentScope = 0;
        this.nextScope = 1;
        this.expectingFunctions = new ArrayList<>();
    }

    public void enterScope() {
        if (!this.symbolTable.containsKey(this.nextScope)) {
            this.symbolTable.put(this.nextScope, new ArrayList<>());
        }
        this.currentScope = this.nextScope;
        this.nextScope++;
    }
    private void addSymbol(String name, String type, DataType dataType, List<Argument> arguments, int currentArraySize) {
        Symbol symbol = new Symbol(name, type, dataType, currentScope, arguments, currentArraySize);
        symbolTable.get(currentScope).add(symbol);
    }
    public void exitScope() {
        this.currentScope = 0;
    }

    public void clearScope() {
        this.currentScope = 0;
        symbolTable.put(0, symbolTable.get(0));
        this.nextScope = 1;
    }

    public void printSymbolTable() {
        final String CYAN_BOLD = "\u001B[36m";
        final String GREEN = "\u001B[32m";
        final String BLUE = "\u001B[34m";
        final String RESET = "\u001B[0m";

        System.out.println("\n" + CYAN_BOLD + "Symbol Table:" + RESET);

        //Noms de cada un dels scopes (0 sempre global, dp ja depenent de l'ordre en els que els posem)
        List<String> functionNames = new ArrayList<>();

        symbolTable.forEach((scopeId, symbols) -> {
            //Scope titles
            if (scopeId == 0) {
                System.out.println("\n" + CYAN_BOLD + "Scope 0: Global scope" + RESET);
            } else {
                String label = "Scope " + scopeId;
                if (scopeId <= functionNames.size()) {
                    label += ": " + functionNames.get(scopeId - 1);
                }
                System.out.println("\n" + CYAN_BOLD + label + RESET);
            }

            //Simbols de cada scope
            symbols.forEach(s -> {
                String color = "FUNC".equals(s.getType()) ? GREEN : BLUE;

                System.out.print("- " + color + s.getName());
                if (s.getArraySize() != 0) {
                    System.out.print("[" + s.getArraySize() + "]");
                }
                System.out.print(RESET + " (" + s.getType() + ", " + s.getDataType() + ")");
                if ("FUNC".equals(s.getType())) {
                    List<Argument> args = s.getArguments();
                    if (args.isEmpty()) {
                        System.out.print(" : NO ARGS");
                    } else {
                        System.out.print(" : ");
                        for (int i = 0; i < args.size(); i++) {
                            Argument arg = args.get(i);
                            System.out.print(arg.getDataType() + " " + arg.getName());
                            if (arg.getArraySize() != 0) {
                                System.out.print("[" + arg.getArraySize() + "]");
                            }
                            if (i < args.size() - 1) System.out.print(", ");
                        }
                    }
                }
                System.out.println();

                // Afegim les funcions de l'scope [NOMÉS APLICA AL SCOPE 0 BTW!] a la llista!
                if (scopeId == 0 && "FUNC".equals(s.getType())) {
                    functionNames.add(s.getName());
                }
            });

        });
    }

    public Symbol getSymbolByName(String name) {
        List<Symbol> currentSymbols = symbolTable.get(this.currentScope);
        for (Symbol s : currentSymbols) {
            if (s.getName().equals(name)) {
                return s;
            }
        }

        if (currentScope != 0) {
            List<Symbol> globalSymbols = symbolTable.get(0);
            for (Symbol s : globalSymbols) {
                if (s.getName().equals(name)) {
                    return s;
                }
            }
        }
        return null;
    }

    private boolean uniqueNameVar(String name) {
        AtomicBoolean unique = new AtomicBoolean(true);
        symbolTable.get(currentScope).forEach(s->{
            if(name.equals(s.getName()) && s.getType().equals("VAR")) {
                unique.set(false);
            }
        });
        if(currentScope !=0) {
            symbolTable.get(0).forEach(s->{
                if(name.equals(s.getName()) && s.getType().equals("VAR")) {
                    unique.set(false);
                }
            });
        }
        return unique.get();
    }
    private boolean uniqueNameFunc(String name) {
        AtomicBoolean unique = new AtomicBoolean(true);
        symbolTable.get(currentScope).forEach(s->{
            if(name.equals(s.getName()) && s.getType().equals("FUNC")) {
                unique.set(false);
            }
        });
        return unique.get();
    }
    private boolean uniqueNameArg() {
        Set<String> names = new HashSet<>();
        for (Argument arg : arguments) {
            if (!names.add(arg.getName())) {
                return false;
            }
        }
        return true;
    }
    private boolean nameAcceptedVar(String name) {
        try {
            FileInputStream fis = new FileInputStream(birraNamePath);
            JSONTokener jsonTokener = new JSONTokener(fis);
            JSONArray jsonArray = new JSONArray(jsonTokener);

            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getString(i).equalsIgnoreCase(name)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("\u001B[31mERROR! NOMBRE VARIABLE NO VALIDA\u001B[0m");
        }

        return false;
    }

    private boolean nameAcceptedArg() {
        for (Argument arg : arguments) {
            if (!nameAcceptedVar(arg.getName())) {
                return false;
            }
        }
        return true;
    }
    private boolean nameAcceptedFunc(String name) {
        try {
            FileInputStream fis = new FileInputStream(nombreBaresPath);
            JSONTokener jsonTokener = new JSONTokener(fis);
            JSONArray jsonArray = new JSONArray(jsonTokener);

            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getString(i).equalsIgnoreCase(name)) {
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("\u001B[31mERROR! NOMBRE FUNCION NO VALIDA\u001B[0m");
        }
        return false;
    }

    public void processSymbol(AbstractMap.SimpleEntry<Token, String> tokenEntry) throws IllegalSymbolException {
        Token token = tokenEntry.getKey();
        String name = tokenEntry.getValue();

        if(token.token.equals("OB")) {
            contCB++;
        }
        if(token.token.equals("CB")) {
            contCB--;
        }

        if(declareMain(token)) {
            return;
        }
        if(declareFunc(token,name)) {
            return;
        }
        if(declareVar(token,name)) {
            return;
        }
        if(closingScope(token)) {
            return;
        }

    }
    private boolean declareVar(Token token, String name) throws IllegalSymbolException {
        if(token.token.equals("OL")) {
            expectingA=true;
            currentArraySize=0;
            return true;
        } else if (expectingA) {
            if(token.token.equals("A")) {
                expectingA = false;
                expectingVarType = true;
            }else{
                expectingA=false;
            }
            return true;
        } else if (expectingVarType && DataType.isDataType(token.token)) {
            expectingOf=true;
            expectingVarType=false;
            symbolType=DataType.fromToken(token.token);
            return true;
        }else if (expectingOf && token.token.equals("OF")) {
            expectingOf = false;
            expectingVarId = true;
            return true;
        }else if(expectingVarId && token.token.equals("CTE_ENTER")) {
            expectingVarType=true;
            expectingVarId=false;
            currentArraySize= Integer.parseInt(name);
        }else if (expectingVarId && token.token.equals("ID")) {
            expectingVarId=false;
            symbolName = name;
            if(uniqueNameVar(name) && nameAcceptedVar(name)) {
                addSymbol(symbolName, "VAR", symbolType, new ArrayList<>(),currentArraySize);
            }else {
                //error
                //System.out.println("\u001B[31mERROR! NOMBRE VARIABLE NO VALIDA ("+name+")\u001B[0m");
                throw new IllegalSymbolException("variable");
            }
            return true;
        }
        return false;
    }
    private boolean closingScope(Token token) {
        if (token.token.equals("CB") && contCB==0) {// fin funcion == cambio scope
            exitScope();
            return true;
        }
        return false;
    }
    private boolean declareFunc(Token token, String name) throws IllegalSymbolException {
        if (token.token.equals("FUNC")) {//ESTAN DECLARANDO FUNC
            symbolName = null;
            symbolType = null;
            arguments = new ArrayList<>();
            expectingFuncId = true;
            currentArraySize=0;
            return true;
        } else if (expectingFuncId && token.token.equals("ID")) {
            symbolName = name;
            expectingFuncId = false;
            expectingReturnType = true;
            return true;
        } else if (expectingReturnType && DataType.isDataType(token.token)) {
            symbolType = DataType.fromToken(token.token);
            expectingReturnType = false;
            expectingOpenParen = true;
            return true;
        } else if (expectingOpenParen && token.token.equals("OP")) { // (
            expectingOpenParen = false;
            expectingArgType = true; // Puede haber argumentos o  )
            return true;
        } else if (expectingArgType && token.token.equals("CP")) { // )
            expectingOpenBrace = true;
            expectingArgType = false;
            return true;
        } else if (expectingArgType && isDataType(token.token)) {
            currentArgType = token.token;
            expectingArgName = true;
            expectingArgType = false;
            if (token.token.equals("ARRAY")) {
                expectingSizeArrya = true;
                expectingArgName = false;
            }
            return true;
        }else if(token.token.equals("CTE_ENTER") && expectingSizeArrya) {
            expectingSizeArrya=false;
            expectingArgType=true;
            currentArraySize = Integer.parseInt(name);
        } else if (expectingArgName && token.token.equals("ID")) {
            arguments.add(new Argument(name, DataType.fromToken(currentArgType), currentArraySize));
            expectingArgName = false;
            expectingCommaOrCP = true;
            return true;
        } else if (expectingCommaOrCP && token.token.equals("COMA")) {
            expectingArgType = true;
            expectingCommaOrCP = false;
            return true;
        } else if (expectingCommaOrCP && token.token.equals("CP")) {
            expectingOpenBrace = true;
            expectingCommaOrCP = false;
            return true;
        } else if (expectingOpenBrace && token.token.equals("OB")) {
            // Guardar funcion !
            expectingOpenBrace = false;
            if (uniqueNameFunc(symbolName) && nameAcceptedFunc(symbolName) && uniqueNameArg() && nameAcceptedArg()) {
                addSymbol(symbolName, "FUNC", symbolType, arguments,0);
                this.expectingFunctions.remove(symbolName);
                enterScope(); // entramos scope
                for (Argument arg : arguments) {
                    addSymbol(arg.getName(), "VAR", arg.getDataType(), new ArrayList<>(), arg.getArraySize());
                }
            } else {
                throw new IllegalSymbolException("function");
            }
            return true;
        }
        return false;
    }
    private boolean declareMain(Token token) throws IllegalSymbolException {
        if (token.token.equals("MAIN")) {//declaracion main
            if(uniqueNameFunc("entra bar")) {
                addSymbol("entra bar", "FUNC", DataType.fromToken("VOID"), new ArrayList<>(), 0);
                enterScope();
            }else {
                //error
                //System.out.println("\u001B[31mERROR! NOMBRE FUNCION MAIN NO VALIDA\u001B[0m");
                throw new IllegalSymbolException("main");
            }
            return true;
        }
        return false;
    }
    private boolean isDataType(String token) {
        return token.equals("INT") || token.equals("BOOL") ||
                token.equals("CHAR") || token.equals("FLOAT") ||
                token.equals("DOUBLE") || token.equals("LONG") ||
                token.equals("ARRAY") || token.equals("STRUCT") ||
                token.equals("VOID");
    }

    public boolean isSymbolDefined(String name, boolean func, boolean var) {
        AtomicBoolean defined = new AtomicBoolean(false);
        symbolTable.get(currentScope).forEach(s->{
            if(func && name.equals(s.getName()) && s.getType().equals("FUNC")) {
                defined.set(true);
            }
            if(var && name.equals(s.getName()) && s.getType().equals("VAR")) {
                defined.set(true);
            }
        });
        if(currentScope !=0) {
            symbolTable.get(0).forEach(s->{
                if(func && name.equals(s.getName()) && s.getType().equals("FUNC")) {
                    defined.set(true);
                }
                if(var && name.equals(s.getName()) && s.getType().equals("VAR")) {
                    defined.set(true);
                }
            });
        }
        return defined.get();
    }

    public void addExpectation(String varName) {
        this.expectingFunctions.add(varName);
    }

    public boolean checkPending() {
        return (this.expectingFunctions.isEmpty());
    }

    public ArrayList<String> getPendingList() {
        return this.expectingFunctions;
    }
    public String getPending() {
        return this.expectingFunctions.toString();
    }

    public boolean funcVoid(String funcName) {
        for (int i = this.symbolTable.get(0).size()-1; i >= 0; i--) {
            if(this.symbolTable.get(0).get(i).getName().equals(funcName)) {
                return this.symbolTable.get(0).get(i).getDataType().getToken().equals("VOID");
            }
            //if (this.symbolTable.get(0).get(i).getType().equals("FUNC")) {
            //    return this.symbolTable.get(0).get(i).getDataType().getToken().equals("VOID");
            //}
        }
        return true;
    }
    public boolean isReturnTypeOK(String varName) {
        //1. Extract var type
        String type = "";
        for (int i = 0; i < this.symbolTable.get(currentScope).size(); i++) {
            if (this.symbolTable.get(currentScope).get(i).getName().equals(varName)) {
                type = this.symbolTable.get(currentScope).get(i).getDataType().getToken();
                break;
            }
        }
        if (Objects.equals(type, "")) {
            for (int i = 0; i < this.symbolTable.get(0).size(); i++) {
                if (this.symbolTable.get(0).get(i).getName().equals(varName)) {
                    type = this.symbolTable.get(0).get(i).getDataType().getToken();
                    break;
                }
            }
        }
        //2. Look for type match
        int index = 0;
        for (int i = 0; i < this.symbolTable.get(0).size(); i++) {
            if (this.symbolTable.get(0).get(i).getType().equals("FUNC")) {
                index++;
                if (index == currentScope) {
                    return this.symbolTable.get(0).get(i).getDataType().getToken().equals(type);
                }
            }
        }
        return false;
    }

    public int argumentsFor(String function) {
        for (int i = 0; i < this.symbolTable.get(0).size(); i++) {
            if (this.symbolTable.get(0).get(i).getName().equals(function)) {
                return this.symbolTable.get(0).get(i).getArguments().size();
            }
        }
        if (this.expectingFunctions.contains(function)) return -1;
        else return -2;
    }

    public boolean isIndexTypeOK(String varName, ArrayList<String> types) {
        //1. Extract var type
        String type = "";
        for (int i = 0; i < this.symbolTable.get(currentScope).size(); i++) {
            if (this.symbolTable.get(currentScope).get(i).getName().equals(varName)) {
                Symbol symbol = this.symbolTable.get(currentScope).get(i);
                type = symbol.getDataType().getToken();
                if (type.equals(DataType.ARRAY.getToken())) {
                    type = String.valueOf(symbol.getValue().getClass().getComponentType()).toUpperCase();
                }
                break;
            }
        }
        if (Objects.equals(type, "")) {
            for (int i = 0; i < this.symbolTable.get(0).size(); i++) {
                if (this.symbolTable.get(0).get(i).getName().equals(varName)) {
                    type = this.symbolTable.get(0).get(i).getDataType().getToken();
                    break;
                }
            }
        }
        return types.contains(type);
    }

    public int getCurrentScope() {
        return currentScope;
    }

    public int getScopeOfVar(String varName) {
        //if not in scope0, it means it's in currScope
        for (int i = 0; i < this.symbolTable.get(0).size(); i++) {
            if (this.symbolTable.get(0).get(i).getName().equals(varName)) { //No cal diferenciar entre vars i funcs ja que per semàntica ja es controla que això no passi :^)
                return 0;
            }
        }
        return this.getCurrentScope();
    }
}
