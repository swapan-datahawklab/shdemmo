package com.example.shelldemo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.StringJoiner;

/**
 * Data Transfer Object (DTO) representing the result of a database connection test.
 */
public class DatabaseLoginServiceTestResult {
    private static final Logger logger = LogManager.getLogger(DatabaseLoginServiceTestResult.class);
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_HEADER = "Database,Service,Type,Success,Response Time (ms),Error Message,SQL State";

    private final String databaseName;
    private final String serviceName;
    private final String dbType;
    private final boolean success;
    private final long responseTimeMs;
    private final String errorMessage;
    private final String sqlState;

    private DatabaseLoginServiceTestResult(Builder builder) {
        this.databaseName = builder.databaseName;
        this.serviceName = builder.serviceName;
        this.dbType = builder.dbType;
        this.success = builder.success;
        this.responseTimeMs = builder.responseTimeMs;
        this.errorMessage = builder.errorMessage;
        this.sqlState = builder.sqlState;
        
        logger.debug("Created ConnectionTestResult: success={}, database={}, service={}, type={}, responseTime={}ms", 
            success, databaseName, serviceName, dbType, responseTimeMs);
        if (!success) {
            logger.debug("Test failed with error: {}, SQL state: {}", errorMessage, sqlState);
        }
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDbType() {
        return dbType;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSqlState() {
        return sqlState;
    }

    public static String getCsvHeader() {
        return CSV_HEADER;
    }

    public String toCsvRecord() {
        logger.trace("Converting ConnectionTestResult to CSV record");
        StringJoiner joiner = new StringJoiner(CSV_DELIMITER)
            .add(escapeField(databaseName))
            .add(escapeField(serviceName))
            .add(escapeField(dbType))
            .add(String.valueOf(success))
            .add(String.valueOf(responseTimeMs))
            .add(escapeField(errorMessage))
            .add(escapeField(sqlState));
        return joiner.toString();
    }

    private String escapeField(String field) {
        if (field == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains delimiter
        String escaped = field.replace("\"", "\"\"");
        return field.contains(CSV_DELIMITER) ? "\"" + escaped + "\"" : escaped;
    }

    @Override
    public String toString() {
        return String.format("ConnectionTestResult{database=%s, service=%s, type=%s, success=%s, responseTime=%dms%s}",
            databaseName, serviceName, dbType, success, responseTimeMs,
            success ? "" : ", error='" + errorMessage + "', sqlState='" + sqlState + "'");
    }

    /**
     * Builder for ConnectionTestResult.
     */
    public static class Builder {
        private String databaseName;
        private String serviceName;
        private String dbType;
        private boolean success;
        private long responseTimeMs;
        private String errorMessage;
        private String sqlState;

        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder responseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder error(String message, String sqlState) {
            this.errorMessage = message;
            this.sqlState = sqlState;
            return this;
        }

        public DatabaseLoginServiceTestResult build() {
            validate();
            return new DatabaseLoginServiceTestResult(this);
        }

        private void validate() {
            if (databaseName == null || databaseName.trim().isEmpty()) {
                String errorMessage = "Database name cannot be null or empty";
                throw new IllegalArgumentException(errorMessage);
            }
            if (serviceName == null || serviceName.trim().isEmpty()) {
                String errorMessage = "Service name cannot be null or empty";
                throw new IllegalArgumentException(errorMessage);
            }
            if (dbType == null || dbType.trim().isEmpty()) {
                String errorMessage = "Database type cannot be null or empty";
                throw new IllegalArgumentException(errorMessage);
            }
            if (!success && (errorMessage == null || errorMessage.trim().isEmpty())) {
                String errorMessage = "Error message is required for failed tests";
                throw new IllegalArgumentException(errorMessage);
            }
            logger.debug("ConnectionTestResult builder validation successful");
        }
    }
} 