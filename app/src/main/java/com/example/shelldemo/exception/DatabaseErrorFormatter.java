// New error handling structure:
package com.example.shelldemo.exception;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.shelldemo.config.ConfigurationHolder;

public class DatabaseErrorFormatter {
    private final String dbType;
    private final Map<String, Map<Integer, DatabaseException.ErrorType>> vendorErrorMappings;

    public DatabaseErrorFormatter(String dbType) {
        this.dbType = dbType.toLowerCase();
        this.vendorErrorMappings = loadVendorMappings();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<Integer, DatabaseException.ErrorType>> loadVendorMappings() {
        Map<String, Object> config = ConfigurationHolder.getInstance().getDatabaseConfig(dbType);
        Map<String, Object> errorMappings = (Map<String, Object>) config.get("error-mappings");
        
        if (errorMappings == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<Integer, DatabaseException.ErrorType>> mappings = new HashMap<>();
        errorMappings.forEach((vendor, codes) -> {
            Map<Integer, String> vendorCodes = (Map<Integer, String>) codes;
            mappings.put(vendor, vendorCodes.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> DatabaseException.ErrorType.valueOf(e.getValue())
                )));
        });
        return mappings;
    }

    public DatabaseException format(SQLException e) {
        String dbmsError = extractDbmsError(e);
        DatabaseException.ErrorType errorType = determineErrorType(e);
        String context = determineContext(e);
        
        return new DatabaseException(
            e.getMessage(),
            errorType,
            dbmsError,
            context
        );
    }

    private String extractDbmsError(SQLException e) {
        if (e.getSQLState() == null && e.getErrorCode() == 0) return null;
        return String.format("SQLState: %s, ErrorCode: %d", 
            e.getSQLState(), 
            e.getErrorCode());
    }

    private DatabaseException.ErrorType determineErrorType(SQLException e) {
        // First try using standard SQLState codes
        String sqlState = e.getSQLState();
        if (sqlState != null) {
            return switch (sqlState.substring(0, 2)) {
                case "08" -> DatabaseException.ErrorType.CONN_FAILED;  // Connection errors
                case "28" -> DatabaseException.ErrorType.CONN_AUTH;    // Auth errors
                case "42" -> DatabaseException.ErrorType.PARSE_SQL;    // Syntax errors
                case "23" -> DatabaseException.ErrorType.OP_QUERY;     // Constraint violations
                default -> handleVendorSpecific(e);
            };
        }
        return handleVendorSpecific(e);
    }

    private DatabaseException.ErrorType handleVendorSpecific(SQLException e) {
        return vendorErrorMappings
            .getOrDefault(dbType, Collections.emptyMap())
            .getOrDefault(e.getErrorCode(), DatabaseException.ErrorType.UNKNOWN);
    }

    private String determineContext(SQLException e) {
        StringBuilder context = new StringBuilder();
        if (e.getSQLState() != null) {
            context.append("SQLState: ").append(e.getSQLState());
        }
        if (e.getErrorCode() != 0) {
            if (context.length() > 0) context.append(", ");
            context.append("VendorCode: ").append(e.getErrorCode());
        }
        return context.length() > 0 ? context.toString() : null;
    }

    // Add similar methods for other DBMS types...
}

