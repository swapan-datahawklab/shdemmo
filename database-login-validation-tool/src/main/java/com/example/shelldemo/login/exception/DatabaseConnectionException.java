package com.example.shelldemo.login.exception;

/**
 * Exception thrown when database connection operations fail.
 */
public class DatabaseConnectionException extends RuntimeException {
    
    public DatabaseConnectionException(String message) {
        super(message);
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
} 