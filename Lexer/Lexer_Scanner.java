package Lexer;

import Parser.ParserErrorHandler;
import dictionary.Dictionary;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer_Scanner {
    private String path;
    private Scanner fileScanner;
    private Dictionary dictionary;
    public ParserErrorHandler handler;
    private boolean verbalize;

    private String remainingLine; // Track the remaining portion of the current line

    public Lexer_Scanner(String path, Dictionary dictionary, ParserErrorHandler handler, boolean verbalize) {
        this.path = path;
        this.dictionary = dictionary;
        this.handler = handler;
        this.verbalize = verbalize;
        this.remainingLine = null;
        openFile();
    }

    private void openFile() {
        try {
            fileScanner = new Scanner(new BufferedReader(new FileReader(this.path)));
            fileScanner.useDelimiter("\n"); // Tokenize by newline
        } catch (FileNotFoundException e) {
            System.err.println("Error opening file: " + e.getMessage());
        }
    }

    public SimpleEntry<Token, String> getNext(ArrayList<Token> expectedTokens) {
        while ((remainingLine == null || remainingLine.trim().isEmpty()) && fileScanner.hasNext()) {
            remainingLine = fileScanner.next();
            this.handler.nextLine();
        }

        // EOF
        if (remainingLine == null || remainingLine.trim().isEmpty()) return null;

        // First we must check for expected tokens, known from F&F
        for (int i = 0; i < expectedTokens.size(); i++) {
            String regex = expectedTokens.get(i).regex;
            Pattern pattern = Pattern.compile("^(" + regex+")");
            Matcher matcher = pattern.matcher(remainingLine);

            if (matcher.find()) {
                String matchedText = matcher.group(); // actual symbol matched
                remainingLine = remainingLine.substring(matchedText.length()).trim(); // Update remaining line
                this.handler.setSymbol(matchedText);
                return new SimpleEntry<>(expectedTokens.get(i), matchedText);
            } else {
                if (verbalize) {
                    System.out.println("\u001B[33mIncorrect regex... checking another!\u001B[0m");
                }
            }
        }

        // If no match we KNOW it's a lexical error... regardless we must identify this token so we explore the whole array
        if (verbalize) System.out.println("\u001B[31mUnexpected token... reporting error & checking whole dictionary!\u001B[0m");
        for (Map.Entry<String, String> entry : dictionary.dictionary.entrySet()) {
            if (entry.getKey().equals("ID")) continue;

            Pattern pattern = Pattern.compile("^(" + entry.getValue()+")");
            Matcher matcher = pattern.matcher(remainingLine);

            if (matcher.find()) {
                String matchedText = matcher.group(); // actual symbol matched
                remainingLine = remainingLine.substring(matchedText.length()).trim(); // Update remaining line
                this.handler.setSymbol(matchedText);
                return new SimpleEntry<>(new Token(entry.getKey(), entry.getValue()), matchedText);
            } else {
                if (verbalize) {
                    System.out.println("\u001B[33mIncorrect regex... checking another!\u001B[0m");
                }
            }
        }

        // Check for the "ID" token last, otherwise it would mistake many tokens for an ID
        String idRegex = dictionary.dictionary.get("ID");
        Pattern idPattern = Pattern.compile("^(" + idRegex + ")");
        Matcher idMatcher = idPattern.matcher(remainingLine);
        if (idMatcher.find()) {
            String matchedText = idMatcher.group(); // actual symbol matched
            remainingLine = remainingLine.substring(matchedText.length()).trim(); // Update remaining line
            this.handler.setSymbol(matchedText);
            return new SimpleEntry<>(new Token("ID", idRegex), matchedText);
        }

        System.out.println("\u001B[31mUnknown token... reporting error!\u001B[0m");
        this.handler.reportLexicalError();
        return null; // Return unknown token if no match found
    }

}
