package com.example.shelldemo.exception;

public class DriverLoadException extends RuntimeException {
    public DriverLoadException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DriverLoadException(String message) {
        super(message);
    }
}