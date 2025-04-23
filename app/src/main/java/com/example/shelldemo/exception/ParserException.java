package com.example.shelldemo.exception;

public class ParserException extends DatabaseException {
    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParserException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ParserException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }
}