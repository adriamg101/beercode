package ParserTable;

import Exceptions.ParserTableException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ParserTableGenerator {
    // Taula de parser
    private HashMap<String, HashMap<String, Integer>> parser_table;
    private final HashMap<String, ArrayList<ArrayList<String>>> grammar;
    public HashMap<String, Set<String>> follows;

    // Paths dels fitxers
    private static final String GRAMMAR_PATH = "gramatica/gramatica.json";
    private static final String PARSER_TABLE_PATH = "ParserTable/parserTable.json";

    // Axioma del codi
    private final String AXIOMA;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";

    /**
     * Function that initializes the parser table.
     * The grammar file must exist.
     * @param redo Flag that indicates to redo the table automatically.
     */
    public ParserTableGenerator(HashMap<String, Set<String>> follows, boolean redo, boolean verbalize, boolean verb_parser_table, String AXIOMA) throws ParserTableException {
        if (verbalize) System.out.println(ANSI_CYAN + "\nParse table init:" + ANSI_RESET);
        grammar = extractGrammarFromFile(verbalize);
        File table_file = new File(PARSER_TABLE_PATH);
        this.follows = follows;
        this.AXIOMA = AXIOMA;

        try {
            // Comprobem si hem de extreure la taula de parser o hem de referla.
            if (redo || checkLastModifications(table_file)) {
                if (verbalize) System.out.println(ANSI_YELLOW + "Generating parse table from scratch..." + ANSI_RESET);
                redoFromScratch(table_file, verbalize, verb_parser_table);
            } else {
                if (verbalize) System.out.println(ANSI_YELLOW + "Extracting parse table from file " + PARSER_TABLE_PATH + "..." + ANSI_RESET);
                extractFromFile(table_file, verbalize);
            }
        } catch (ParserTableException e) {
            throw new ParserTableException(e.getMessage());
        }
    }

    /**
     * Function that returns the parser table.
     * @return ParseTable object with the grammar and the parser table.
     */
    public ParserTable getParserTable() {
        return new ParserTable(grammar, parser_table);
    }

    /**
     * Function that writes the parser table json file with the contents of parser_table argument.
     * @param table_file File from the parser table.
     */
    private void writeParserTableInFile(File table_file) throws ParserTableException {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        try {
            FileWriter writer = new FileWriter(table_file);
            gson.toJson(parser_table, writer);
            writer.close();
        } catch (IOException e) {
            throw new ParserTableException("ERROR: Error ocurred while trying to write in the parser table json file.");
        }
    }

    /**
     * Function that recalculates all the parser table from the given grammar file. It saves the resulting parser table
     * in a new file.
     * @param table_file parser_table file to store the new parser table in it.
     * @throws ParserTableException Throws the exception if there has been a problem in the functions.
     */
    private void redoFromScratch(File table_file, boolean verbalize, boolean verb_parser_table) throws ParserTableException {
        parser_table = new HashMap<>();

        try {
            ArrayList<ParserTableStackNode> arrTree = new ArrayList<>();

            // Primer afegim al stack l'axioma
            if (verbalize) System.out.println(ANSI_YELLOW + "Start generating all grammar combinations..." + ANSI_RESET);
            if (verb_parser_table) System.out.println(
                    "Combinations found: [" + ANSI_BLUE + "non-terminal" + ANSI_RESET + ", " + ANSI_CYAN + "first" + ANSI_RESET + "]"
            );
            arrTree.add(new ParserTableStackNode(AXIOMA));

            // Hem de mirar tots els tokens no terminals
            checkUntilNextTerminal(arrTree);
            while (!arrTree.isEmpty()) {
                evaluateTerminal(arrTree, verb_parser_table);
                checkUntilNextTerminal(arrTree);
            }
            if (verbalize) System.out.println(ANSI_GREEN + "Parse table generated from scratch." + ANSI_RESET);

            // Write the contents in a new json
            writeParserTableInFile(table_file);
        } catch (ParserTableException e) {
            throw new ParserTableException(e.getMessage());
        }
    }

    /**
     * Function that evaluates all the non-terminal tokens and modify the arrTree to check all the possible combinations.
     * @param arrTree ArrayList that have the tree to evaluate the grammar.
     */
    private void checkUntilNextTerminal(ArrayList<ParserTableStackNode> arrTree) {
        // Anem afegint les produccions al arbre mentre el següent token a evaluar no sigui terminal
        String actual_token = arrTree.get(arrTree.size()-1).getActualToken();
        while (!arrTree.isEmpty() && (actual_token == null || grammar.containsKey(actual_token))) {
            // Afegim la produccio del token
            if (actual_token != null && arrTree.get(arrTree.size()-1).getProductionPos() < grammar.get(actual_token).size()) {
                ArrayList<String> prod = grammar.get(actual_token).get(arrTree.get(arrTree.size()-1).getProductionPos());

                // Comprobem que la produccio no estigui en el arrTree
                boolean can_add = true;
                int i = arrTree.size() - 1;
                while (can_add && i >= 0) {
                    if (arrTree.get(i).equalsProd(prod)) can_add = false;
                    i--;
                }

                if (can_add) {
                    // Si la produccio no es repeteix la afegim al arrTree
                    arrTree.add(new ParserTableStackNode(prod));
                } else {
                    // Si es repeteix fem skip d'aquesta
                    arrTree.get(arrTree.size()-1).nextProductionPos();
                }
            } else {
                // Si es null significa que hem de eliminar la produccio
                if (actual_token == null) {
                    // token overflow
                    arrTree.remove(arrTree.size()-1);
                    if (!arrTree.isEmpty()) arrTree.get(arrTree.size()-1).nextProductionPos();
                } else {
                    // production overflow
                    arrTree.get(arrTree.size()-1).nextToken();
                }
            }
            if (!arrTree.isEmpty()) actual_token = arrTree.get(arrTree.size()-1).getActualToken();
        }
    }

    /**
     * Function that evaluates a terminal and looks if their parents have to be set with it.
     * @param arrTree ArrayList that have the tree to evaluate the grammar.
     * @throws ParserTableException Throws the exception if there are multiple productions with the same non-terminal
     * and first.
     */
    private void evaluateTerminal(ArrayList<ParserTableStackNode> arrTree, boolean verb_parser_tree) throws ParserTableException {
        int i = arrTree.size() - 1;
        String first = arrTree.get(arrTree.size()-1).getActualToken();

        // Si el token es epsilon hem de utilitzar els follows del token del pare
        if (first.equals("ε")) {
            evaluateEpsilon(arrTree.get(arrTree.size() - 2).getActualToken(), arrTree.get(arrTree.size() - 2).getProductionPos(), verb_parser_tree);
            arrTree.get(arrTree.size()-1).nextToken();
            return;
        }

        // Afegim a la taula de parser totes les combinacions que el token es el seu first
        while (i > 0 && arrTree.get(i).isFirstToken()) {
            // Si el token actual es el first, anem al pare i marquem que el seu first es el first de tot
            i--;
            if (!setProduction(arrTree.get(i).getActualToken(), first, arrTree.get(i).getProductionPos(), verb_parser_tree)) {
                // Si dos produccions arriben a una mateixa combinacio de non-terminal i first es un error de la gramatica
                throw new ParserTableException(
                        "ERROR: There are multiple productions with the same non-terminal and first:\n" +
                                "\t- Non-terminal token: " + arrTree.get(i).getActualToken() + "\n" +
                                "\t- First: " + first + "\n"
                );
            }
        }

        // Quan hem acabat avancem el token
        arrTree.get(arrTree.size()-1).nextToken();
    }


    private void evaluateEpsilon(String epsilon_father, int production_pos, boolean verbalize) {
        // Si el token terminal es un epsilon significa que hem de buscar els follows del pare per a la producció.
        ArrayList<String> epsilon_follows = new ArrayList<>(follows.get(epsilon_father));

        for (int i = 0; i < epsilon_follows.size(); i++) {
            setProduction(epsilon_father, epsilon_follows.get(i), production_pos, verbalize);
        }
    }

    /**
     * Function that reads the contents of the grammar json file and returns it.
     * @return HashMap<String, ArrayList<String>> containing the grammar.
     */
    private HashMap<String, ArrayList<ArrayList<String>>> extractGrammarFromFile(boolean verbalize) throws ParserTableException {
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, ArrayList<ArrayList<String>>>>() {}.getType();

        if (verbalize) System.out.println(ANSI_YELLOW + "Extracting grammar from grammar file " + GRAMMAR_PATH + "..." + ANSI_RESET);
        try {
            FileReader reader = new FileReader(GRAMMAR_PATH);
            HashMap<String, ArrayList<ArrayList<String>>> grammar = gson.fromJson(reader, type);
            reader.close();
            if (verbalize) System.out.println(ANSI_GREEN + "Grammar successfully extracted." + ANSI_RESET);
            return grammar;
        } catch (IOException e) {
            throw new ParserTableException("ERROR: Error ocurred while trying to read in the grammar json file.");
        }
    }

    /**
     * Function that extracts the table from the json file.
     * @param table_file Table parser file opened.
     */
    private void extractFromFile(File table_file, boolean verbalize) throws ParserTableException {
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, HashMap<String, Integer>>>() {}.getType();

        try {
            FileReader reader = new FileReader(table_file);
            parser_table = gson.fromJson(reader, type);
            reader.close();
            if (verbalize) System.out.println(ANSI_GREEN + "Parse table extracted from file successfully." + ANSI_RESET);
        } catch (IOException e) {
            throw new ParserTableException("ERROR: Error ocurred while trying to read in the parser table json file.");
        }
    }

    /**
     * Function if the grammar file have been modified before or after the parser table file.
     * If the parser table file was modified before we need to redo the parser table (so it returns true).
     * @param table_file Parser table file.
     * @return boolean indicating if we need to redo the parser table.
     */
    private boolean checkLastModifications(File table_file) {
        // Mirem si el fitxer de taula de parser existeix
        if (!table_file.exists()) return true;

        // Suposem que els fitxers existeixen
        long grammar_last = new File(GRAMMAR_PATH).lastModified();

        // Mirem si els fitxers de grammar i de first i follows son mes nous que el de la taula de parser
        return table_file.lastModified() < grammar_last;
    }

    /**
     * Function that sets a position in the parser table.
     * @param non_terminal_token Non-terminal token for the production.
     * @param first First that generates the production for the non-terminal-token.
     * @param production_pos Position of the production in the grammar HashMap.
     * @return Boolean that indicates that there wasn't any production already
     */
    private boolean setProduction(String non_terminal_token, String first, int production_pos, boolean verbalize) {
        // Setejem la fila de non-terminal si no existeix
        parser_table.computeIfAbsent(non_terminal_token, k -> new HashMap<>());

        // Primer mirem si no hi ha una produccio
        if (parser_table.get(non_terminal_token).containsKey(first)
                && parser_table.get(non_terminal_token).get(first) != production_pos) {
            return false;
        }

        // Mirem si la produccio ja esta en la taula, si ho esta no la tornem a afegir
        if (!parser_table.get(non_terminal_token).containsKey(first)) {
            parser_table.computeIfAbsent(non_terminal_token, k -> new HashMap<>());
            parser_table.get(non_terminal_token).put(first, production_pos);

            if (verbalize) System.out.println(
                    "- [" + ANSI_BLUE + non_terminal_token + ANSI_RESET + ", " + ANSI_CYAN + first + ANSI_RESET + "] : "
                            + grammar.get(non_terminal_token).get(parser_table.get(non_terminal_token).get(first))
            );
        }

        return true;
    }
}
