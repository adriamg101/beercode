package gramatica;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Grammar {

    private static final HashMap<String, List<List<String>>> grammar = new HashMap<>();

    public static String getStartSymbol() {
        if (!grammar.isEmpty()) {
            return grammar.keySet().iterator().next();
        }
        return null;
    }

    public void generateGrammar(String grammar_path) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(grammar_path);
        JSONTokener jsonTokener = new JSONTokener(inputStream);
        JSONObject jsonGrammar = new JSONObject(jsonTokener);

        for (String nonTerminal : jsonGrammar.keySet()) {
            JSONArray productions = jsonGrammar.getJSONArray(nonTerminal);
            List<List<String>> productionList = new ArrayList<>();

            for (int i = 0; i < productions.length(); i++) {
                JSONArray production = productions.getJSONArray(i);
                List<String> symbols = new ArrayList<>();

                for (int j = 0; j < production.length(); j++) {
                    symbols.add(production.getString(j));
                }

                productionList.add(symbols);
            }

            grammar.put(nonTerminal, productionList);
        }
    }


    // Retorna la gramàtica
    public static HashMap<String, List<List<String>>> getGrammar() {
        return grammar;
    }

    public void displayGrammar() {
        for (String nonTerminal : grammar.keySet()) {
            System.out.println(nonTerminal + " -> ");
            List<List<String>> productions = grammar.get(nonTerminal);
            for (List<String> production : productions) {
                System.out.println("    " + String.join(" ", production));
            }
        }
    }

    public void destroy() {
        this.grammar.clear();
    }

    public boolean isNoTerminal(String key) {
        return grammar.containsKey(key);
    }
}
