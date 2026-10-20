package Parser;

import Exceptions.*;
import Lexer.Lexer_Scanner;
import Lexer.Token;
import ParserTable.ParserTable;
import Semmantic.SemmanticChecker;
import Semmantic.SymbolTable;
import ParsingTree.ParsingTreeNavigator;


import java.util.*;

public class Parser {
    private Lexer_Scanner scanner;
    private HashMap<String, String> tokenList;
    HashMap<String, Set<String>> firsts;
    HashMap<String, Set<String>> follows;
    private ParserTable parser_table;
    private Stack<String> parsingStack;
    private SymbolTable symbolTable;
    private boolean visualize;
    private ParsingTreeNavigator parsingTreeNavigator;
    private SemmanticChecker semantic;

    public Parser(SymbolTable symbolTable, SemmanticChecker sem, Lexer_Scanner scanner, HashMap<String, String> tokenList, HashMap<String, Set<String>> firsts, HashMap<String, Set<String>> follows, ParserTable parser_table, ParsingTreeNavigator parsingTreeNavigator, boolean visualize) {
        this.scanner = scanner;
        this.tokenList = tokenList;
        this.firsts = firsts;
        this.follows = follows;
        this.parser_table = parser_table;
        this.parsingTreeNavigator = parsingTreeNavigator;
        this.parsingStack = new Stack<>();
        parsingStack.push(this.parsingTreeNavigator.getCurrentToken());
        this.symbolTable = symbolTable;
        this.semantic = sem;
        this.visualize = visualize;
    }

    public void start() {
        System.out.println("\n\u001B[36mParser init:\u001B[0m");
        AbstractMap.SimpleEntry<Token, String> nextWord = null;

        do {
            //Based on stack, we KNOW which tokens to expect next!! --> Thus making it predictive :)
            ArrayList<Token> t = getNextExpectedTokens(nextWord);

            nextWord = this.scanner.getNext(t);
            if (nextWord == null) return;
            if (visualize) System.out.println("\u001B[32mNext token: " + nextWord.getKey().token + "\u001B[0m");
            try {
                symbolTable.processSymbol(nextWord);
            } catch (IllegalSymbolException e) {
                String top = this.parsingStack.pop();
                String curr = this.parsingStack.peek();

                if (!curr.startsWith("<")) {
                    if (!nextWord.getValue().matches(tokenList.get(curr))) {
                        this.parsingStack.push(top);
                    }
                }

                this.scanner.handler.reportIllegalSymbol(e.getMessage());
            }
        } while (nextWord != null);
    }

    /**
     * Funcion that constructs stack and check expected tokens to make scanner predictive
     * @param prevWord Last word scanned, used for lookahead
     * @return List of tokens to look for
     */
    private ArrayList<Token> getNextExpectedTokens(AbstractMap.SimpleEntry<Token, String> prevWord) {
        if (prevWord == null) return new ArrayList<>();

        //DENSE FUNCION!
        //1. Take top element in stack. If null OR NULLABLE, non-functional non-terminal then pop another one
        // (for example <VARIABLE'> is nullable so if it doesn't find a production we just skip it!!)
        String next = parsingStack.pop();
        if (Objects.equals(next, "ε")) next = parsingStack.pop();
        //2. If its a non-terminal, recursively repeat operation
        if (next.charAt(0) == '<') {
            ArrayList<String> productions = parser_table.getProduction(next, prevWord.getKey().token);
            if (productions == null) {
                this.handleError(next);
                Set<String> followSet = this.follows.get(next);
                do {
                    //If current token is in the FOLLOW set...
                    if (followSet != null && followSet.contains(prevWord.getKey().token)) {
                        parsingStack.pop();
                        return new ArrayList<>();
                    }
                    // Otherwise, skip :)
                    prevWord = this.scanner.getNext(new ArrayList<>());
                } while (prevWord != null && !followSet.contains(prevWord.getKey().token));
                parsingStack.pop();
                return new ArrayList<>();
            }
            for (int i = productions.size()-1; i >= 0; i--) {
                parsingStack.push(productions.get(i));
                checkSemantic(productions.get(i), "");
            }
            // Add all the children of the production to the node
            this.parsingTreeNavigator.addChildren(productions, next);
            return getNextExpectedTokens(prevWord);
        } else {
            //If terminal + match...
            if (next.equals(prevWord.getKey().token) || prevWord.getValue().matches(tokenList.get(next))) { //Second part is for token mismatch! Will only be evaluated if error :)
                //Confirmem token, anirem a buscar els seguents de manera predictiva!!
                //Si el proxim element de l'stack és un terminal, l'anirem a buscar tal cual
                //Sinó, anirem a buscar els firsts del no terminal (i si son NULLABLE els follows també >.<')
                this.parsingTreeNavigator.setTokenValue(prevWord.getValue(), next);
                checkSemantic(next, prevWord.getValue());
                if (parsingStack.peek().charAt(0) != '<') {
                    ArrayList<Token> arr = new ArrayList<>();
                    arr.add(new Token(parsingStack.peek(), tokenList.get(parsingStack.peek())));
                    if (visualize) System.out.println("Checking next character... it should be: "+parsingStack.peek());
                    return arr;
                } else {
                    Set<String> f = this.firsts.get(parsingStack.peek());
                    Object[] s = f.toArray();
                    ArrayList<Token> arr = new ArrayList<>();
                    for (int i = 0; i < f.size(); i++) {
                        arr.add(new Token((String) s[i], tokenList.get((String)s[i])));
                    }
                    if (f.contains("ε")) {
                        Set<String> fl = this.follows.get(parsingStack.peek());
                        Object[] sl = fl.toArray();
                        for (int i = 0; i < fl.size(); i++) {
                            arr.add(new Token((String) sl[i], tokenList.get((String)sl[i])));
                        }
                        if (visualize) System.out.println("Checking next character... it should be one of: "+ Arrays.toString(s) + ", " + Arrays.toString(sl));
                    } else {
                        if (visualize) System.out.println("Checking next character... it should be one of: "+ Arrays.toString(s));
                    }
                    return arr;
                }
            } else {
                this.handleError(next);
                next = this.parsingStack.pop();
                ArrayList<String> productions = parser_table.getProduction(next, prevWord.getKey().token);
                if (productions == null) {
                    return new ArrayList<>();
                }
                for (int i = productions.size()-1; i >= 0; i--) {
                    parsingStack.push(productions.get(i));
                }
                // Add all the children of the production to the node
                this.parsingTreeNavigator.addChildren(productions, next);
                return getNextExpectedTokens(prevWord);
            }
        }
    }

    private void checkSemantic (String next, String varName) {
        try {
            semantic.checkNextToken(next, varName);
        } catch (UndefinedSymbolException e) {
            this.scanner.handler.reportUndefinedVariable(e.getMessage());
        } catch (UndefinedFunctionException e) {
            this.scanner.handler.reportUndefinedFunction(e.getMessage());
        }catch (noReturnException e) {
            this.scanner.handler.noReturnException(e.getMessage());
        } catch (TypeMismatchException e) {
            this.scanner.handler.reportTypeMismatch(e.getMessage());
        } catch (incorrectCloseLineSymbolException e) {
            this.scanner.handler.reportIncorrectCLSymbol(e.getMessage());
        } catch (ArgumentCountException e) {
            this.scanner.handler.reportArgumentCountMismatch(e.getMessage(), 0);
        } catch (RecordPhantomArgumentsException e) {
            this.scanner.handler.recordPhantomArguments(e.funcName, e.argN);
        } catch (IndexMismatchException e) {
            this.scanner.handler.reportIndexMismatch(e.getMessage());
        }
    }

    private void handleError(String tok){
        this.scanner.handler.reportSyntaxError(this.tokenList.get(tok));
        //this.parsingTreeNavigator.setError(tok);  //TODO: DEBUG --> but it's only to print it pretty so it's not critical :)
    }
}
