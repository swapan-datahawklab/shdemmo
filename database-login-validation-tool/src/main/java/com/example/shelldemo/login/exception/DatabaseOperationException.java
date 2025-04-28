package com.example.shelldemo.login.exception;

/**
 * Exception thrown when a database operation fails.
 */
public class DatabaseOperationException extends RuntimeException {
    
    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 