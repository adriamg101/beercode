package dictionary;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Dictionary {


    public final HashMap<String, String> dictionary;


    public Dictionary() {
        this.dictionary = new HashMap<>();
    }

    /**
     * Generates the dictionary from the dicitonary.json file
     *
     * @param dictionary_path the path to the dictionary.json file
     */
    public void generate(String dictionary_path) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(dictionary_path);
        JSONTokener jsonTokener = new JSONTokener(inputStream);
        JSONObject dictionaryJson = new JSONObject(jsonTokener);

        Iterator<String> keys = dictionaryJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = dictionaryJson.getString(key);
            this.dictionary.put(key, value);
        }
    }

    /**
     * Checks if the string is a regex pattern
     * @param str The string to check
     * @return True if the string is a regex pattern
     */
    private boolean isRegexPattern(String str) {
        return str.matches(".*[()\\[\\]|+*?{}\\\\].*") ||
                str.startsWith("\\") ||
                str.contains("|");
    }

    /**
     * Checks if the token is valid from a given lexeme and an input String
     *
     * @param token        Is the real token (commonly this will be the one given by the First Function
     * @param input_lexeme Is the input of the code given by the scanner commonly
     * @return Return True in case that the String (input_lexeme) is the same as the token
     */
    public boolean isValidTokenFromFirst(String token, String input_lexeme) {
        String lexeme = this.dictionary.get(token);
        if (lexeme == null) return false;

        String fullPattern = lexeme.contains("|") ? "(" + lexeme + ")" : lexeme;
        return Pattern.matches(fullPattern, input_lexeme);
    }

    /**
     * Checks if the token is valid from a given lexeme and an input String, established priorities for the tokens
     * this ensures that the token ID is the last one to be checked
     * @param input_lexeme Is the input of the code given by the scanner commonly
     * @return Return True in case that the String (input_lexeme) is the same as the token
     */
    public String isValidToken(String input_lexeme) {
        for (Map.Entry<String, String> entry : this.dictionary.entrySet()) {
            if (entry.getKey().equals("ID")) continue;

            String lexeme = entry.getValue();
            if (!isRegexPattern(lexeme) && lexeme.equals(input_lexeme)) {
                return entry.getKey();
            }

            if (isRegexPattern(lexeme) && Pattern.matches(lexeme, input_lexeme)) {
                return entry.getKey();
            }
        }

        String idLexeme = this.dictionary.get("ID");
        if (idLexeme != null && Pattern.matches(idLexeme, input_lexeme)) return "ID";

        return null;
    }

    /**
     * Display the dictionary, just for debugging purposes
     */
    public void display() {
        for (String key : this.dictionary.keySet()) {
            System.out.println(key + " : " + this.dictionary.get(key));
        }
    }

    public void test_example() {
        String[] nextFirst = {"MAIN", "OB", "ID", "CL", "UAIL", "lala"}; // Això es el que hauria de predir amb el predictor de X (X pk ns qui hauria de ser)
        String[] code_input = {"entra bar", "🤢", "hola", "🤮", "apunta mentre", "chupito"}; // Aixo es el que m'hauria de donar el scanner

        // Check if the token is valid
        int i = 0;
        for (String input : code_input) {
            if (isValidTokenFromFirst(nextFirst[i], input)) {
                System.out.println("The token: " + input + " is valid with token: " + nextFirst[i]);
            } else {
                String token = isValidToken(input);
                if (token != null) {
                    System.out.println("Bad first! The token: " + input + " is valid with token: " + token);
                } else {
                    System.out.println("The token: " + input + " is not valid");
                }
            }
            i++;
        }
    }

    /**
     * Destroy the dictionary
     */
    public void destroy() {
        this.dictionary.clear();
    }
}
