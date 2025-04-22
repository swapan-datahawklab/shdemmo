package com.example.shelldemo.model.dto.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Optional;

/**
 * DTO representing the result of a database connection test.
 * Provides comprehensive information about the test outcome including
 * timing, error details, and connection metadata.
 */
public class ConnectionTestResult {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionTestResult.class);
    private final String databaseName;
    private final String serviceName;
    private final String dbType;
    private final boolean success;
    private final Instant timestamp;
    private final long responseTimeMs;
    private final String errorMessage;
    private final String errorCode;

    private ConnectionTestResult(Builder builder) {
        logger.trace("Creating ConnectionTestResult for database: {}", builder.databaseName);
        this.databaseName = builder.databaseName;
        this.serviceName = builder.serviceName;
        this.dbType = builder.dbType;
        this.success = builder.success;
        this.timestamp = builder.timestamp;
        this.responseTimeMs = builder.responseTimeMs;
        this.errorMessage = builder.errorMessage;
        this.errorCode = builder.errorCode;

        if (success) {
            logger.debug("Connection test successful for database: {} ({}ms)", databaseName, responseTimeMs);
        } else {
            logger.debug("Connection test failed for database: {} - Error: {} (Code: {})", 
                databaseName, errorMessage, errorCode);
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public Optional<String> getErrorCode() {
        return Optional.ofNullable(errorCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("ConnectionTestResult{")
            .append("database='").append(databaseName).append("', ")
            .append("service='").append(serviceName).append("', ")
            .append("type='").append(dbType).append("', ")
            .append("success=").append(success).append(", ")
            .append("responseTime=").append(responseTimeMs).append("ms");

        if (errorMessage != null) {
            sb.append(", error='").append(errorMessage).append("'");
        }
        if (errorCode != null) {
            sb.append(", errorCode='").append(errorCode).append("'");
        }
        
        return sb.append("}").toString();
    }

    /**
     * Converts the result to a CSV record.
     * @return CSV formatted string of the test result
     */
    public String toCsvRecord() {
        logger.trace("Converting connection test result to CSV record for database: {}", databaseName);
        String record = String.format("%s,%s,%s,%s,%d,%s,%s,%s",
            databaseName,
            serviceName,
            dbType,
            success ? "SUCCESS" : "FAILED",
            responseTimeMs,
            timestamp,
            errorCode != null ? errorCode : "",
            errorMessage != null ? errorMessage.replace(",", ";") : "");
        logger.trace("Generated CSV record for database {}: {}", databaseName, record);
        return record;
    }

    /**
     * @return CSV header for the test results
     */
    public static String getCsvHeader() {
        return "Database,ServiceName,Type,Status,ResponseTime(ms),Timestamp,ErrorCode,ErrorMessage";
    }

    public static class Builder {
        private static final Logger logger = LoggerFactory.getLogger(ConnectionTestResult.Builder.class);
        private String databaseName;
        private String serviceName;
        private String dbType;
        private boolean success;
        private Instant timestamp = Instant.now();
        private long responseTimeMs;
        private String errorMessage;
        private String errorCode;

        public Builder databaseName(String databaseName) {
            logger.trace("Setting database name: {}", databaseName);
            validateNotNull("databaseName", databaseName);
            this.databaseName = databaseName;
            return this;
        }

        public Builder serviceName(String serviceName) {
            logger.trace("Setting service name: {}", serviceName);
            validateNotNull("serviceName", serviceName);
            this.serviceName = serviceName;
            return this;
        }

        public Builder dbType(String dbType) {
            logger.trace("Setting database type: {}", dbType);
            validateNotNull("dbType", dbType);
            this.dbType = dbType;
            return this;
        }

        public Builder success(boolean success) {
            logger.trace("Setting success: {}", success);
            this.success = success;
            return this;
        }

        public Builder responseTimeMs(long responseTimeMs) {
            logger.trace("Setting response time: {}ms", responseTimeMs);
            if (responseTimeMs < 0) {
                String errorMessage = "Response time cannot be negative: " + responseTimeMs;
                logger.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder error(String message, String code) {
            logger.trace("Setting error - Message: {}, Code: {}", message, code);
            if (message != null && code == null || message == null && code != null) {
                String errorMessage = "Both error message and code must be either null or non-null";
                logger.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            this.errorMessage = message;
            this.errorCode = code;
            return this;
        }

        private void validateNotNull(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                String errorMessage = fieldName + " cannot be null or empty";
                logger.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }

        private void validateRequiredFields() {
            logger.trace("Validating required fields for database: {}", databaseName);
            validateNotNull("databaseName", databaseName);
            validateNotNull("serviceName", serviceName);
            validateNotNull("dbType", dbType);
            
            if (!success && (errorMessage == null || errorCode == null)) {
                String errorMessage = "Error details required for failed connection test";
                logger.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
            
            if (success && (errorMessage != null || errorCode != null)) {
                String errorMessage = "Error details should not be present for successful connection test";
                logger.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }

        public ConnectionTestResult build() {
            logger.debug("Building ConnectionTestResult for database: {}", databaseName);
            validateRequiredFields();
            return new ConnectionTestResult(this);
        }
    }
} 