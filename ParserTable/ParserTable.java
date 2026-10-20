package ParserTable;

import java.util.ArrayList;
import java.util.HashMap;

public class ParserTable {
    // Taula de parser
    private final HashMap<String, HashMap<String, Integer>> parser_table;
    private final HashMap<String, ArrayList<ArrayList<String>>> grammar;

    public ParserTable(HashMap<String, ArrayList<ArrayList<String>>> grammar, HashMap<String, HashMap<String, Integer>> parser_table) {
        this.grammar = grammar;
        this.parser_table = parser_table;
    }

    /**
     * getProduction() returns the production of the given token with the respective first. If there isn't any
     * production associated with the first it returns null.
     * @param non_terminal_token Next token to evaluate.
     * @param first Next token read by the scanner.
     * @return An array of tokens with the production or null if it doesn't exist.
     */
    public ArrayList<String> getProduction(String non_terminal_token, String first) {
        // Si el first no existeix retornem null
        if (!parser_table.get(non_terminal_token).containsKey(first)) return null;

        // Si existeix retornem la produccio
        return grammar.get(non_terminal_token).get(parser_table.get(non_terminal_token).get(first));
    }
}
