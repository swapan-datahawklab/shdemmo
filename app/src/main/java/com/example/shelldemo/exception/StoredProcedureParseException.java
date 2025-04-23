package com.example.shelldemo.exception;

public class StoredProcedureParseException extends ParserException {
    public static final String ERROR_CODE_INVALID_FORMAT = "PARSE_001";
    public static final String ERROR_CODE_MISSING_VALUE = "PARSE_002";
    public static final String ERROR_CODE_INVALID_PARAM = "PARSE_003";

    public StoredProcedureParseException(String message) {
        super(message, ERROR_CODE_INVALID_FORMAT);
    }

    public StoredProcedureParseException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE_INVALID_FORMAT);
    }

    public StoredProcedureParseException(String message, String errorCode) {
        super(message, errorCode);
    }

    public StoredProcedureParseException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }
} 