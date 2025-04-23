// New error handling structure:
package com.example.shelldemo.error;

import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.DatabaseOperationType;

public class DatabaseErrorFormatter {
    private final String dbType;
    private static final Logger logger = LogManager.getLogger(DatabaseErrorFormatter.class);

    public DatabaseErrorFormatter(String dbType) {
        this.dbType = dbType.toLowerCase();
    }

    /**
     * Formats database errors based on database type
     */
    public String formatError(SQLException e) {
        try {
            return switch (dbType) {
                case "oracle" -> formatOracleError(e);
                case "postgresql" -> formatPostgresError(e);
                case "mysql" -> formatMySqlError(e);
                case "sqlserver" -> formatSqlServerError(e);
                default -> e.getMessage();
            };
        } catch (Exception ex) {
            logger.warn("Error while formatting database error", ex);
            return e.getMessage();
        }
    }

    /**
     * Formats Oracle specific errors
     */
    private String formatOracleError(SQLException e) {
        String message = e.getMessage();
        int oraIndex = message.indexOf("ORA-");
        if (oraIndex >= 0) {
            int endIndex = message.indexOf(":", oraIndex);
            String oraCode = endIndex > oraIndex ? 
                message.substring(oraIndex, endIndex) : 
                message.substring(oraIndex);
            String errorDetails = message.substring(endIndex + 1).trim();
            return String.format("%s: %s", oraCode, errorDetails);
        }
        return message;
    }

    /**
     * Formats PostgreSQL specific errors
     */
    private String formatPostgresError(SQLException e) {
        return String.format("PostgreSQL Error %s: %s", 
            e.getSQLState(), 
            e.getMessage()
        );
    }

    /**
     * Formats MySQL specific errors
     */
    private String formatMySqlError(SQLException e) {
        return String.format("MySQL Error %d: %s", 
            e.getErrorCode(), 
            e.getMessage()
        );
    }

    /**
     * Formats SQL Server specific errors
     */
    private String formatSqlServerError(SQLException e) {
        return String.format("SQL Server Error %d: %s", 
            e.getErrorCode(), 
            e.getMessage()
        );
    }

    /**
     * Creates context-aware error messages for different operations
     */
    public String formatWithContext(SQLException e, DatabaseOperationType operationType, 
            String additionalContext) {
        String baseError = formatError(e);
        return String.format("%s error during %s: %s. Context: %s", 
            dbType.toUpperCase(), 
            operationType, 
            baseError, 
            additionalContext
        );
    }
}

