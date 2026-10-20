package ParserTable;

import java.util.ArrayList;

public class ParserTableStackNode {
    private final ArrayList<String> production;   // Produccio
    private int actual_token;   // Actual token a evaluar
    private int prod_pos;    // Actual produccio del fill actual

    /**
     * Node from the ArrayList arrTree. It works like a stack.
     * @param production Production to be stored.
     */
    public ParserTableStackNode(ArrayList<String> production) {
        this.production = production;
        prod_pos = 0;
        actual_token = 0;
    }

    /**
     * Node from the ArrayList arrTree. Only used with the axioma.
     * @param token Token to be stored in the production.
     */
    public ParserTableStackNode(String token) {
        production = new ArrayList<>();
        production.add(token);
        prod_pos = 0;
        actual_token = 0;
    }

    /**
     * Function that returns the actual token being evaluated. If the token is overflowing, it returns null.
     * @return The String of the token being evaluated. If there's overflow, it returns null.
     */
    public String getActualToken() {
        if (actual_token < production.size()) return production.get(actual_token);
        return null;
    }

    /**
     * Function that increments the pointer of the token and sets the production position to zero.
     */
    public void nextToken() {
        actual_token++;
        prod_pos = 0;
    }

    /**
     * Function that returns the production position.
     * @return Integer of the production position.
     */
    public int getProductionPos() {
        return prod_pos;
    }

    /**
     * Function that increments the production position.
     */
    public void nextProductionPos() {
        prod_pos++;
    }

    /**
     * Function to check if we are looking at the first token of the production.
     * @return Boolean indicating if the actual token is the first one.
     */
    public boolean isFirstToken() {
        return actual_token == 0;
    }

    /**
     * Function that checks if the production entered by parameter is the same as the one in the class.
     * @param prod ArrayList of the production to check.
     * @return Boolean indicating if the production is the same.
     */
    public boolean equalsProd(ArrayList<String> prod) {
        return production.equals(prod);
    }
}
