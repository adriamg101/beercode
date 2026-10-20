package Lexer;

public class Token {
    public String token;
    public String regex;

    public Token(String token, String regex) {
        this.token = token;
        this.regex = regex;
    }

    public String getToken() {
        return token;
    }

    public String getRegex() {
        return regex;
    }
}
