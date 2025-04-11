package com.example.shelldemo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base exception class for all database-related exceptions.
 * Provides common logging and context handling functionality.
 */
public class DatabaseException extends RuntimeException {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseException.class);
    private final String errorCode;
    private final String context;

    public DatabaseException(String message) {
        this(message, null, null, null);
    }

    public DatabaseException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public DatabaseException(String message, String errorCode) {
        this(message, null, errorCode, null);
    }

    public DatabaseException(String message, Throwable cause, String errorCode) {
        this(message, cause, errorCode, null);
    }

    public DatabaseException(String message, Throwable cause, String errorCode, String context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
        logException();
    }

    private void logException() {
        String logMessage = buildLogMessage();
        if (getCause() != null) {
            logger.error(logMessage, getCause());
        } else {
            logger.error(logMessage);
        }
    }

    private String buildLogMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        
        if (errorCode != null) {
            sb.append(" [Error Code: ").append(errorCode).append("]");
        }
        
        if (context != null) {
            sb.append(" [Context: ").append(context).append("]");
        }
        
        return sb.toString();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getContext() {
        return context;
    }
} 