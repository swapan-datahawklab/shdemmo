package com.example.shelldemo.exception;

import java.io.File;

/**
 * Exception thrown when errors occur during SQL script parsing.
 */
public class SqlParseException extends ParserException {
    public static final String ERROR_CODE_INVALID_FORMAT = "SQL_PARSE_001";
    public static final String ERROR_CODE_FILE_IO_ERROR = "SQL_PARSE_002";
    public static final String ERROR_CODE_INVALID_STATEMENT = "SQL_PARSE_003";
    
    public SqlParseException(String message) {
        super(message, ERROR_CODE_INVALID_FORMAT);
    }
    
    public SqlParseException(String message, Throwable cause) {
        super(message, cause, ERROR_CODE_INVALID_FORMAT);
    }
    
    public SqlParseException(String message, String errorCode) {
        super(message, errorCode);
    }
    
    public SqlParseException(String message, Throwable cause, String errorCode) {
        super(message, cause, errorCode);
    }
    
    /**
     * Validates that a SQL script file exists and is accessible.
     * 
     * @param scriptFile The SQL script file to validate
     * @throws SqlParseException if the file is null, doesn't exist, or isn't a file
     */
    public static void validateScriptFile(File scriptFile) throws SqlParseException {
        if (scriptFile == null) {
            throw new SqlParseException("Script file cannot be null", 
                ERROR_CODE_INVALID_FORMAT);
        }
        if (!scriptFile.exists()) {
            throw new SqlParseException("Script file does not exist: " + scriptFile.getPath(), 
                ERROR_CODE_INVALID_FORMAT);
        }
        if (!scriptFile.isFile()) {
            throw new SqlParseException("Path is not a file: " + scriptFile.getPath(), 
                ERROR_CODE_INVALID_FORMAT);
        }
    }
}
