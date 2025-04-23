package com.example.shelldemo.exception;

/**
 * Exception thrown when Oracle-specific connection operations fail.
 * Provides specific error codes and context for Oracle connection failures.
 */
public class OracleConnectionException extends DatabaseConnectionException {
    public static final String ERROR_CODE_INVALID_THIN = "ORA_CONN_001";
    public static final String ERROR_CODE_INVALID_LDAP = "ORA_CONN_002";
    public static final String ERROR_CODE_MISSING_LDAP_CONFIG = "ORA_CONN_003";
    public static final String ERROR_CODE_INVALID_SERVICE = "ORA_CONN_004";

    public OracleConnectionException(String message) {
        super(message, ERROR_CODE_INVALID_THIN);
    }

    public OracleConnectionException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE_INVALID_THIN);
    }

    public OracleConnectionException(String message, String errorCode) {
        super(message, errorCode);
    }

    public OracleConnectionException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }

    public OracleConnectionException(String message, Throwable cause, String errorCode, String context) {
        super(message, cause, errorCode, context);
    }
} 