package com.example.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.example.shelldemo.model.ConnectionConfig;

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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "host=" + config.getHost() +
                ", port=" + config.getPort() +
                ", database=" + config.getDatabase() +
                ", username=" + config.getUsername() +
                "}";
    }
}