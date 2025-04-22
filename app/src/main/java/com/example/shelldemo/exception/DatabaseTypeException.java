package com.example.shelldemo.exception;

/**
 * Exception thrown when there are database type validation errors.
 * Provides specific error codes and context for type-related failures.
 */
public class DatabaseTypeException extends DatabaseException {
    public static final String ERROR_CODE_INVALID_TYPE = "DB_TYPE_001";
    public static final String ERROR_CODE_UNSUPPORTED = "DB_TYPE_002";
    public static final String ERROR_CODE_DRIVER_MISSING = "DB_TYPE_003";

    public enum ErrorType {
        UNSUPPORTED,
        DRIVER_MISSING,
        INVALID
    }

    public DatabaseTypeException(String message, ErrorType type) {
        super(message, switch(type) {
            case UNSUPPORTED -> ERROR_CODE_UNSUPPORTED;
            case DRIVER_MISSING -> ERROR_CODE_DRIVER_MISSING;
            case INVALID -> ERROR_CODE_INVALID_TYPE;
        });
    }

    public DatabaseTypeException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE_INVALID_TYPE);
    }

    public DatabaseTypeException(String message, String errorCode) {
        super(message, errorCode);
    }

    public DatabaseTypeException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public DatabaseTypeException(String message, Throwable cause, String errorCode, String context) {
        super(message, cause, errorCode, context);
    }

} 