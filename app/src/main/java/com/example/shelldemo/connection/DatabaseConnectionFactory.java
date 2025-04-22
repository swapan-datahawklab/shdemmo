package com.example.shelldemo.connection;

import com.example.shelldemo.config.ConfigurationHolder;
import com.example.shelldemo.config.ConfigurationException;
import com.example.shelldemo.exception.DatabaseConnectionException;
import com.example.shelldemo.exception.DatabaseTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Collections;
import java.util.HashMap;

/**
 * Factory class for creating database connections.
 * Supports different database types and connection methods using templates.
 */
public class DatabaseConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionFactory.class);
    private final ConfigurationHolder configHolder;

    public DatabaseConnectionFactory(ConfigurationHolder configHolder) {
        this.configHolder = configHolder;
    }

    /**
     * Gets a database connection based on the provided configuration.
     *
     * @param config The connection configuration
     * @return A database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection(ConnectionConfig config) throws SQLException {
        String dbType = config.getDbType().toLowerCase();
        if (!configHolder.isValidDbType(dbType)) {
            throw new DatabaseTypeException("Unsupported database type: " + dbType, DatabaseTypeException.ERROR_CODE_UNSUPPORTED);
        }

        String connectionUrl = getConnectionUrl(config);
        Properties props = getConnectionProperties(config);

        logger.info("Attempting to connect to {} using URL: {}", dbType, connectionUrl);
        try {
            return DriverManager.getConnection(connectionUrl, props);
        } catch (SQLException e) {
            throw handleConnectionException(dbType, e, config);
        }
    }

    private RuntimeException handleConnectionException(String dbType, SQLException e, ConnectionConfig config) {
        if ("oracle".equals(dbType)) {
            try {
                // Dynamically load OracleConnectionException
                Class<?> oracleExceptionClass = Class.forName("com.example.shelldemo.connection.oracle.OracleConnectionException");
                String errorCode = determineOracleErrorCode(e, config);
                return (RuntimeException) oracleExceptionClass
                    .getConstructor(String.class, Throwable.class, String.class)
                    .newInstance("Failed to connect to Oracle database", e, errorCode);
            } catch (ReflectiveOperationException ex) {
                logger.warn("OracleConnectionException not available, falling back to DatabaseConnectionException");
            }
        }
        return new DatabaseConnectionException("Failed to connect to database", e, DatabaseConnectionException.ERROR_CODE_CONNECTION_FAILED);
    }

    private String determineOracleErrorCode(SQLException e, ConnectionConfig config) {
        String message = e.getMessage().toLowerCase();
        String connectionType = config.getConnectionType();
        
        if (message.contains("ldap") || ("ldap".equals(connectionType) && message.contains("connection"))) {
            return "ORA_CONN_002"; // ERROR_CODE_INVALID_LDAP
        } else if (message.contains("thin") || ("thin".equals(connectionType) && message.contains("connection"))) {
            return "ORA_CONN_001"; // ERROR_CODE_INVALID_THIN
        } else if (message.contains("service")) {
            return "ORA_CONN_004"; // ERROR_CODE_INVALID_SERVICE
        }
        return "ORA_CONN_001"; // Default to INVALID_THIN
    }

    private String getConnectionUrl(ConnectionConfig config) {
        String dbType = config.getDbType().toLowerCase();
        String connectionType = config.getConnectionType() != null ? config.getConnectionType().toLowerCase() : "thin";

        Map<String, Map<String, Object>> dbTypes = configHolder.getDatabaseTypes();
        Map<String, Object> typeConfig = dbTypes.get(dbType);
        
        if (typeConfig == null) {
            throw new DatabaseConnectionException("No configuration found for database type: " + dbType,
                DatabaseConnectionException.ERROR_CODE_INVALID_CONFIG);
        }

        @SuppressWarnings("unchecked")
        Map<String, String> templates = typeConfig.get("templates") instanceof Map ? 
            new HashMap<>((Map<String, String>) typeConfig.get("templates")) : 
            Collections.emptyMap();

        String urlTemplate = templates.get(connectionType);
        if (urlTemplate == null) {
            urlTemplate = (String) typeConfig.get("urlTemplate");
            if (urlTemplate == null) {
                throw new DatabaseConnectionException(
                    String.format("No URL template configured for database type: %s and connection type: %s", dbType, connectionType),
                    DatabaseConnectionException.ERROR_CODE_INVALID_CONFIG
                );
            }
        }

        return String.format(urlTemplate, config.getHost(), config.getPort(), config.getServiceName());
    }

    private Properties getConnectionProperties(ConnectionConfig config) {
        String dbType = config.getDbType().toLowerCase();
        Properties props = new Properties();
        
        Map<String, Map<String, Object>> dbTypes = configHolder.getDatabaseTypes();
        Map<String, Object> typeConfig = dbTypes.get(dbType);
        
        if (typeConfig == null) {
            throw new DatabaseConnectionException("No configuration found for database type: " + dbType,
                DatabaseConnectionException.ERROR_CODE_INVALID_CONFIG);
        }

        // Get connection type specific properties first
        String connectionType = config.getConnectionType() != null ? config.getConnectionType().toLowerCase() : "default";
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = typeConfig.get("properties") instanceof Map ?
            new HashMap<>((Map<String, Object>) typeConfig.get("properties")) :
            Collections.emptyMap();

        // If there are connection-type specific properties, apply them
        @SuppressWarnings("unchecked")
        Map<String, Object> typeProperties = properties.get(connectionType) instanceof Map ?
            (Map<String, Object>) properties.get(connectionType) :
            Collections.emptyMap();
            
        // Apply connection-type specific properties
        typeProperties.forEach((key, value) -> props.put(key, value.toString()));
        
        // Apply common properties for this database type
        properties.forEach((key, value) -> {
            if (!(value instanceof Map)) {  // Skip nested property maps
                props.put(key, value.toString());
            }
        });

        // Add authentication properties if provided
        if (config.getUsername() != null) {
            props.setProperty("user", config.getUsername());
        }
        if (config.getPassword() != null) {
            props.setProperty("password", config.getPassword());
        }

        return props;
    }

    /**
     * Validates if the given database type is supported.
     *
     * @param dbType The database type to validate
     * @return true if the database type is valid, false otherwise
     */
    public boolean isValidDbType(String dbType) {
        logger.trace("Validating database type: {}", dbType);
        try {
            return configHolder.isValidDbType(dbType);
        } catch (ConfigurationException e) {
            logger.error("Failed to validate database type: {}", e.getMessage(), e);
            throw new DatabaseTypeException("Failed to validate database type", e);
        }
    }

    /**
     * Gets the default port for the given database type.
     *
     * @param dbType The database type
     * @return The default port number
     */
    public int getDefaultPort(String dbType) {
        logger.trace("Getting default port for database type: {}", dbType);
        try {
            return configHolder.getDefaultPort(dbType);
        } catch (ConfigurationException e) {
            logger.error("Failed to get default port: {}", e.getMessage(), e);
            throw new DatabaseTypeException("Failed to get default port", e);
        }
    }
}