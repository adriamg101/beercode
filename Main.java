import Exceptions.ParserTableException;
import Exceptions.UndefinedFunctionException;
import Lexer.Lexer_Scanner;
import Mips.*;
import Optimitzador.Optimitzador;
import Parser.Parser;
import Parser.ParserErrorHandler;
import ParserTable.ParserTable;
import ParserTable.ParserTableGenerator;
import ParsingTree.ParsingTree;
import ParsingTree.ParsingTreeNavigator;
import Semmantic.SemmanticChecker;
import Semmantic.SymbolTable;
import ThreeAddressCode.TAC;
import dictionary.Dictionary;
import gramatica.FirstAlgorithm;
import gramatica.Grammar;
import preprocesado.preprocesado;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;

public class Main {

    final static String DICTIONARY_PATH = "dictionary/dictionary.json";
    final static String GRAMMAR_PATH = "gramatica/gramatica.json";
    final static String OUTPUT_FILE = "output/output.asm";

    // General flags
    final static String FLAG_VERBALIZE_LONG = "--verbalize";
    final static String FLAG_VERBALIZE_SHORT = "-v";

    // Flags parser table
    final static String FLAG_VIS_PARSER_TABLE_LONG = "--verbalize-parse-table";
    final static String FLAG_VIS_PARSER_TABLE_SHORT = "-v-pt";
    final static String FLAG_REDO_LONG = "--redo-parse-table";
    final static String FLAG_REDO_SHORT = "-re-pt";

    // Flags parser-scanner
    final static String FLAG_VIS_PARSER = "-v-ps";

    // Flags symbol table
    final static String FLAG_VIS_SYMBOL_TABLE = "-v-st";

    // Flags parsing tree
    final static String FLAG_VIS_PARSING_TREE = "-v-t";
    final static String FLAG_OPT_PARSING_TREE = "-o-t";

    // Flags TAC
    final static String FLAG_VIS_TAC = "-v-tac";

    // Flags Optimitzador
    final static String FLAG_O1 = "-o";
    final static String FLAG_V_O = "-v-o";

    // Constants
    // This is a placeholder for the terminals that the value is needed.
    final static String CONSTANTS = "<VALUE>";
    final static int WORD_SIZE = 4;
    final static int MAX_REGS = 24;

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(ANSI_RED + "Error: You must provide a file as the first argument." + ANSI_RESET);
            return;
        }
        File file = new File(args[0]);
        if (!file.exists()) {
            System.out.println(ANSI_RED + "Error: The file " + args[0] + " does not exist." + ANSI_RESET);
            return;
        }

        // Setejem els args
        String AXIOMA;
        HashSet<String> args_set = new HashSet<>(Arrays.asList(args).subList(1, args.length));

        //Objects
        Dictionary dictionary = new Dictionary();
        Grammar grammar = new Grammar();
        FirstAlgorithm firstAlgorithm = new FirstAlgorithm();
        ParserTable parser_table;

        //FASE 0: Crear sets + dictionary + tables...
        try {
            dictionary.generate(DICTIONARY_PATH);
            //dictionary.display();

            grammar.generateGrammar(GRAMMAR_PATH);
            //grammar.displayGrammar();

            firstAlgorithm.generateFirstSet();
            //firstAlgorithm.displayFirstSets();
            firstAlgorithm.generateFollowSet();
            //firstAlgorithm.displayFollowSets();

            AXIOMA = firstAlgorithm.getAXIOMA();

            ParserTableGenerator parser_table_generator = new ParserTableGenerator(firstAlgorithm.getFollows(),
                    (args_set.contains(FLAG_REDO_SHORT) || args_set.contains(FLAG_REDO_LONG)),
                    (args_set.contains(FLAG_VERBALIZE_SHORT) || args_set.contains(FLAG_VERBALIZE_LONG)),
                    (args_set.contains(FLAG_VIS_PARSER_TABLE_SHORT) || args_set.contains(FLAG_VIS_PARSER_TABLE_LONG)),
                    AXIOMA
            );
            parser_table = parser_table_generator.getParserTable();

            //dictionary.destroy();
            //grammar.destroy();
        } catch (FileNotFoundException | ParserTableException e) {
            System.out.println(e.getMessage());
            return;
        }

        //FASE 1: PRE-PROCESSAT
        preprocesado.process(args[0]);

        //FASE 2: SCANNER + PARSER + SYMBOL TABLE + PARSE TREE (simultaneous :3)
        ParserErrorHandler handler = new ParserErrorHandler();
        Lexer_Scanner sc = new Lexer_Scanner("preprocesado/processedCode", dictionary, handler, (args_set.contains(FLAG_VERBALIZE_SHORT) || args_set.contains(FLAG_VERBALIZE_LONG)));
        SymbolTable symbolTable = new SymbolTable();
        SemmanticChecker sem = new SemmanticChecker(symbolTable);
        ParsingTree parsingTree = new ParsingTree(AXIOMA);
        ParsingTreeNavigator parsingTreeNavigator = new ParsingTreeNavigator(parsingTree, firstAlgorithm.getConstantsFirst(CONSTANTS), args_set.contains(FLAG_OPT_PARSING_TREE));
        Parser p = new Parser(symbolTable, sem, sc, dictionary.dictionary, firstAlgorithm.firsts, firstAlgorithm.follows, parser_table, parsingTreeNavigator, (args_set.contains(FLAG_VIS_PARSER)));
        p.start();

        try {
            sem.checkPendingSymbols();
        } catch (UndefinedFunctionException e) {
            handler.reportUndefinedFunction(e.getMessage());
        }
        handler.checkPhantoms(symbolTable.getPendingList(), symbolTable.symbolTable);

        System.out.println("\n\u001B[36mParser End: Nothing else to report.\u001B[0m");
        if (args_set.contains(FLAG_VIS_SYMBOL_TABLE)) symbolTable.printSymbolTable();
        if (args_set.contains(FLAG_VIS_PARSING_TREE)) parsingTree.printTree();

        //FASE 3: TACS + OPTIMIZATIONS
        if (handler.getErrorCount() == 0) {
            symbolTable.clearScope();
            TAC tac = new TAC(symbolTable, firstAlgorithm.getConstantsFirst(CONSTANTS), WORD_SIZE);
            tac.generate(parsingTree);
            if (args_set.contains(FLAG_V_O) && args_set.contains(FLAG_O1)) {
                System.out.println("----------------------------\nPre-optimized TAC:\n");
            }
            if (args_set.contains(FLAG_VIS_TAC) ||
                    (args_set.contains(FLAG_V_O) && args_set.contains(FLAG_O1))){
                tac.showTAC();
            }

            // Optimitzador
            Optimitzador optimitzador = new Optimitzador();
            if (args_set.contains(FLAG_O1)) optimitzador.optimizeTAC(tac);
            if (args_set.contains(FLAG_V_O) && args_set.contains(FLAG_O1)) {
                System.out.println("----------------------------\nPost-optimized TAC:\n");
                tac.showTAC();
                System.out.println("----------------------------");
            }

            MMU mmu = new MMU(MAX_REGS, true);

            Mips mips = new Mips(tac.getInstructions(), mmu, WORD_SIZE);
            mips.generateMips(OUTPUT_FILE);
        } else {
            System.out.println("Errors are present, aborting compilation...");
        }
    }
}
