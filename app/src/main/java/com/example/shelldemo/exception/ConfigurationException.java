package com.example.shelldemo.exception;

/**
 * Exception thrown when there are configuration-related errors.
 * Provides specific error codes and context for configuration failures.
 */
public class ConfigurationException extends DatabaseException {
    public static final String ERROR_CODE_MISSING_CONFIG = "DB_CFG_001";
    public static final String ERROR_CODE_INVALID_CONFIG = "DB_CFG_002";
    public static final String ERROR_CODE_FILE_NOT_FOUND = "DB_CFG_003";
    public static final String ERROR_CODE_PARSE_ERROR = "DB_CFG_004";

    public ConfigurationException(String message) {
        super(message, ERROR_CODE_INVALID_CONFIG);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE_INVALID_CONFIG);
    }

    public ConfigurationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ConfigurationException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public ConfigurationException(String message, Throwable cause, String errorCode, String context) {
        super(message, cause, errorCode, context);
    }
} 