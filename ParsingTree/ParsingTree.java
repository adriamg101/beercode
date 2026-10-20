package ParsingTree;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;

public class ParsingTree {
    private ParsingTree parent;
    private final AbstractMap.SimpleEntry<String, String> node;
    private final LinkedList<ParsingTree> children;
    private boolean isError;

    public ParsingTree(String token, String value) {
        this.node = new AbstractMap.SimpleEntry<>(token, value);
        this.children = new LinkedList<>();
        this.parent = null;
    }

    public ParsingTree(String token) {
        this.node = new AbstractMap.SimpleEntry<>(token, null);
        this.children = new LinkedList<>();
        this.parent = null;
    }

    public void setError() {
        this.isError = true;
    }

    /**
     * Function that adds all the children to the current node
     * @param children ArrayList of children to add
     */
    public void addChildren(ArrayList<ParsingTree> children) {
        for (ParsingTree child : children) {
            child.setParent(this);
        }
        this.children.addAll(children);
    }

    public ParsingTree getChild(int index) {
        if (index < 0 || index >= this.children.size()) {
            return this;
        }
        return this.children.get(index);
    }
    public AbstractMap.SimpleEntry<String, String> getNode() {
        return this.node;
    }

    public LinkedList<ParsingTree> getChildren() {
        return this.children;
    }

    public String getToken() {
        return this.node.getKey();
    }

    public void setParent(ParsingTree parent) {
        this.parent = parent;
    }

    public ParsingTree getParent() {
        return this.parent;
    }

    public void setNodeValue(String value) {
        this.node.setValue(value);
    }

    public AbstractMap.SimpleEntry<String, String> getLeaf(ParsingTree node) {
        if (node == null) node = this;

        if (node.getChildren().isEmpty()) {
            return node.getNode();
        }
        for (ParsingTree child : node.getChildren()) {
            AbstractMap.SimpleEntry<String, String> leaf = getLeaf(child);
            if (leaf != null) {
                return leaf;
            }
        }
        return null;
    }

    public ParsingTree getNextNode(ParsingTree node) {
        if (node == null) return null;
        if (!node.getChildren().isEmpty()) {
            return node.getChild(0);
        }
        // VAig als germans
        while (true) {
            ParsingTree parent = node.getParent();
            if (parent == null) return null;

            int index = parent.getChildren().indexOf(node);
            if (index + 1 < parent.getChildren().size()) {
                return parent.getChild(index + 1);
            }
            node = parent;
        }
    }

    public void printTree() {
        System.out.println("\n\u001B[36mParsing Tree:\u001B[0m");
        System.out.println("\u001B[94mNon-Terminal\u001B[0m | " +
                "\u001B[93mGeneralTerminal\u001B[0m | " +
                "\u001B[95mDataType\u001B[0m | " +
                "\u001B[38;5;208mConstant\u001B[0m | " +
                "\u001B[92mId\u001B[0m | " +
                "\u001B[0mDefault (ε) | " +
                "\u001B[91mErrors\u001B[0m");
        printTree("", true);
    }

    private String getColor(String token) {
        final String BLUE = "\u001B[94m";     // Non-Terminals
        final String YELLOW = "\u001B[93m";   // GeneralTerminals
        final String MAGENTA = "\u001B[95m";  // DataTypes
        final String ORANGE = "\u001B[38;5;208m"; // Constants/Literals
        final String GREEN = "\u001B[92m";    // Identifiers
        final String WHITE = "\u001B[0m";     // Default

        if (token.startsWith("<")) {
            return BLUE;
        }
        if (token.matches("ID")) {
            return GREEN;
        }
        if (token.matches("CTE_.*")) {
            return ORANGE;
        }
        if (token.matches("VOID|BOOL|CHAR|INT|FLOAT|DOUBLE|LONG|ARRAY|STRUCT")) {
            return MAGENTA;
        }
        if (token.equals("ε")) {
            return WHITE;
        }
        return YELLOW; //General terminal worlds like main, return, ...
    }

    private void printTree(String prefix, boolean isLast) {
        final String RESET = "\u001B[0m";
        final String RED = "\u001B[91m";

        String color = getColor(this.node.getKey());
        if (this.isError) color = RED;
        String out = prefix + (isLast ? "└── " : "├── ") + color + this.node.getKey();
        if (this.node.getValue() != null) {
            out += " (" + this.node.getValue() + ")";
        }
        System.out.println(out + RESET);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).printTree(prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }

}
