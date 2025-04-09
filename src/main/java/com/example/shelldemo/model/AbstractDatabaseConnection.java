package com.example.shelldemo.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abstract base class for database connections.
 * Provides common functionality for managing database connections.
 */
public abstract class AbstractDatabaseConnection {
    protected final ConnectionConfig config;
    protected final Properties connectionProperties;

    protected AbstractDatabaseConnection(ConnectionConfig config) {
        this.config = config;
        this.connectionProperties = new Properties();
        initializeDefaultProperties();
    }

    protected void initializeDefaultProperties() {
        connectionProperties.setProperty("user", config.getUsername());
        connectionProperties.setProperty("password", config.getPassword());
        
        // Add timeout properties if they are set
        if (config.getConnectionTimeout() > 0) {
            connectionProperties.setProperty("oracle.net.CONNECT_TIMEOUT", 
                String.valueOf(config.getConnectionTimeout()));
        }
        if (config.getReadTimeout() > 0) {
            connectionProperties.setProperty("oracle.jdbc.ReadTimeout", 
                String.valueOf(config.getReadTimeout()));
        }
    }

    protected void addConnectionProperty(String key, String value) {
        connectionProperties.setProperty(key, value);
    }

    protected Properties getConnectionProperties() {
        return connectionProperties;
    }

    protected ConnectionConfig getConfig() {
        return config;
    }

    protected abstract Connection createConnection() throws SQLException;
    protected abstract Connection createConnection(ConnectionConfig config) throws SQLException;

    public Connection getConnection() throws SQLException {
        return createConnection();
    }

    /**
     * Returns a string representation of the connection configuration.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "host=" + config.getHost() +
                ", port=" + config.getPort() +
                ", serviceName=" + config.getServiceName() +
                ", username=" + config.getUsername() +
                ", databaseType=" + config.getDatabaseType() +
                "}";
    }
}