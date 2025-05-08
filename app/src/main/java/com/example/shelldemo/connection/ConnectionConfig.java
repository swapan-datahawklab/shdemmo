package com.example.shelldemo.connection;

import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.config.ConfigurationHolder;

/**
 * Configuration class for database connections.
 * Handles validation and enrichment of connection parameters.
 */
public class ConnectionConfig {
    private static final Logger logger = LogManager.getLogger(ConnectionConfig.class);

    private String host;
    private int port;
    private String username;
    private String password;
    private String serviceName;
    private String dbType;
    private String connectionType;

    // Getters and setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }
    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getConfigMap(Object obj, String key) {
        if (!(obj instanceof Map)) {
            throw new DatabaseException(
                String.format("Expected Map configuration for '%s' but got: %s", 
                    key, 
                    obj != null ? obj.getClass().getSimpleName() : "null"
                ),
                ErrorType.CONFIG_INVALID
            );
        }
        
        Map<String, Object> configMap = (Map<String, Object>) ((Map<?, ?>) obj).get(key);
        if (configMap == null) {
            throw new DatabaseException(
                String.format("Required configuration section '%s' is missing", key),
                ErrorType.CONFIG_NOT_FOUND
            );
        }
        
        return configMap;
    }

    private void enrichPort(Map<String, Object> defaults) {
        if (defaults.containsKey("port") && port <= 0) {
            Object portValue = defaults.get("port");
            if (!(portValue instanceof Number)) {
                throw new DatabaseException(
                    String.format("Invalid port configuration. Expected number but got: %s", 
                        portValue != null ? portValue.getClass().getSimpleName() : "null"
                    ),
                    ErrorType.CONFIG_INVALID
                );
            }
            port = ((Number) portValue).intValue();
        }
    }

    private void enrichConnectionType(Map<String, Object> defaults) {
        if (defaults.containsKey("connection-type")) {
            Object connType = defaults.get("connection-type");
            if (!(connType instanceof String)) {
                throw new DatabaseException(
                    String.format("Invalid connection-type configuration. Expected string but got: %s",
                        connType != null ? connType.getClass().getSimpleName() : "null"
                    ),
                    ErrorType.CONFIG_INVALID
                );
            }
            String defaultConnType = (String) connType;
            if (connectionType == null) {
                logger.info("No connection type specified, using default: {}", defaultConnType);
                connectionType = defaultConnType;
            }
        }
    }

    /**
     * Validates and enriches the configuration with default values.
     * 
     * @throws DatabaseException if validation fails
     * @return this instance for method chaining
     */
    public ConnectionConfig validateAndEnrich() {
        validateRequiredFields();
        
        String validatedDbType = dbType.trim().toLowerCase();
        Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(validatedDbType);
        
        if (dbmsConfig == null || dbmsConfig.isEmpty()) {
            String errorMessage = "Invalid or unsupported database type: " + validatedDbType;
            logger.error(errorMessage);
            throw new DatabaseException(errorMessage, ErrorType.CONFIG_INVALID);
        }

        Map<String, Object> defaults = getConfigMap(dbmsConfig, "defaults");
        enrichPort(defaults);
        enrichConnectionType(defaults);
        dbType = validatedDbType;
        
        return this;
    }

    /**
     * Validates that all required fields are present and valid.
     * 
     * @throws DatabaseException if validation fails
     */
    private void validateRequiredFields() {
        if (dbType == null || dbType.trim().isEmpty()) {
            throw new DatabaseException("Database type must be specified", ErrorType.CONFIG_INVALID);
        }
        if (host == null || host.trim().isEmpty()) {
            throw new DatabaseException("Host must be specified", ErrorType.CONFIG_INVALID);
        }
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new DatabaseException("Service name must be specified", ErrorType.CONFIG_INVALID);
        }
        if (username == null || username.trim().isEmpty()) {
            throw new DatabaseException("Username must be specified", ErrorType.CONFIG_INVALID);
        }
        if (password == null) {
            throw new DatabaseException("Password must be specified", ErrorType.CONFIG_INVALID);
        }
    }

    /**
     * Creates a builder for fluent configuration creation.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating ConnectionConfig instances.
     */
    public static class Builder {
        private final ConnectionConfig config = new ConnectionConfig();

        public Builder host(String host) { config.setHost(host); return this; }
        public Builder port(int port) { config.setPort(port); return this; }
        public Builder username(String username) { config.setUsername(username); return this; }
        public Builder password(String password) { config.setPassword(password); return this; }
        public Builder serviceName(String serviceName) { config.setServiceName(serviceName); return this; }
        public Builder dbType(String dbType) { config.setDbType(dbType); return this; }
        public Builder connectionType(String connectionType) { config.setConnectionType(connectionType); return this; }

        public ConnectionConfig build() {
            return config.validateAndEnrich();
        }
    }
} 