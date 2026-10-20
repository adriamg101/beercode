package Exceptions;

import java.io.IOException;

public class UndefinedFunctionException extends Exception {
    public UndefinedFunctionException(String message) {
        super(message);
    }
}
