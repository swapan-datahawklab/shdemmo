package com.example.shelldemo.exception;

/**
 * Exception thrown when database connection operations fail.
 * Provides specific error codes and context for connection failures.
 */
public class DatabaseConnectionException extends DatabaseException {
    public static final String ERROR_CODE_CONNECTION_FAILED = "DB_CONN_001";
    public static final String ERROR_CODE_TIMEOUT = "DB_CONN_002";
    public static final String ERROR_CODE_AUTHENTICATION = "DB_CONN_003";
    public static final String ERROR_CODE_INVALID_CONFIG = "DB_CONN_004";

    public DatabaseConnectionException(String message) {
        super(message, ERROR_CODE_CONNECTION_FAILED);
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE_CONNECTION_FAILED);
    }

    public DatabaseConnectionException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DatabaseConnectionException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public DatabaseConnectionException(String message, Throwable cause, String errorCode, String context) {
        super(message, cause, errorCode, context);
    }
} 