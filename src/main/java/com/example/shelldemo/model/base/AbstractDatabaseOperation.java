package com.example.shelldemo.model.base;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.shelldemo.model.domain.ConnectionConfig;


public abstract class AbstractDatabaseOperation {
    private final Logger log = LoggerFactory.getLogger(AbstractDatabaseOperation.class);
    

    protected final AbstractDatabaseConnection connection;
    protected abstract AbstractDatabaseConnection createDatabaseConnection(ConnectionConfig config) throws SQLException;

    protected AbstractDatabaseOperation(AbstractDatabaseConnection connection) {
        this.connection = Objects.requireNonNull(connection, "Database connection cannot be null");
    }
    

    protected AbstractDatabaseOperation(ConnectionConfig config) throws SQLException {
        Objects.requireNonNull(config, "Connection configuration cannot be null");
        this.connection = createDatabaseConnection(config);
        log.debug("Created database operation with new connection for {}", config.getHost());
    }
    

    protected AbstractDatabaseOperation(String host, String username, String password) throws SQLException {
        this(createBasicConfig(host, username, password));
    }
    

    private static ConnectionConfig createBasicConfig(String host, String username, String password) {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(host);
        config.setUsername(username);
        config.setPassword(password);
        return config;
    }
    

    


    public <T> T execute(ConnectionCallback<T> callback) throws SQLException {
        log.debug("Executing database operation");
        try (Connection conn = connection.getConnection()) {
            return callback.execute(conn);
        }
    }

    public <T> T executeTransaction(ConnectionCallback<T> callback) throws SQLException {
        log.debug("Executing database operation with transaction");
        Connection conn = null;
        boolean originalAutoCommit = true;
        
        try {
            conn = connection.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            T result = callback.execute(conn);
            conn.commit();
            return result;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException closeEx) {
                    log.warn("Failed to close connection", closeEx);
                }
            }
        }
    }
    

    @FunctionalInterface
    public interface ConnectionCallback<T> {
        T execute(Connection connection) throws SQLException;
    }
}
