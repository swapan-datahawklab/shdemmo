package com.example.shelldemo.exception;

/**
 * Exception thrown when a database operation fails.
 * Provides specific error codes and context for operation failures.
 */
public class DatabaseOperationException extends DatabaseException {
    public static final String ERROR_CODE_QUERY_FAILED = "DB_OP_001";
    public static final String ERROR_CODE_UPDATE_FAILED = "DB_OP_002";
    public static final String ERROR_CODE_PROCEDURE_FAILED = "DB_OP_003";
    public static final String ERROR_CODE_TRANSACTION_FAILED = "DB_OP_004";

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseOperationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DatabaseOperationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public DatabaseOperationException(String message, Throwable cause, String errorCode, String context) {
        super(message, cause, errorCode, context);
    }
} 