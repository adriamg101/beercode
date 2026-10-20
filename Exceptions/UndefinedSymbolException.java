package Exceptions;

import java.io.IOException;

public class UndefinedSymbolException extends Exception {
    public UndefinedSymbolException(String message) {
        super(message);
    }
}
