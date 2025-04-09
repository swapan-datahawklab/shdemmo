package com.example.shelldemo.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for database operations that uses AbstractDatabaseConnection
 * to provide vendor-neutral database operations with proper connection management.
 */
public abstract class AbstractDatabaseOperation {
    private static final Logger log = LoggerFactory.getLogger(AbstractDatabaseOperation.class);
    
    // Use composition to leverage AbstractDatabaseConnection
    protected final AbstractDatabaseConnection connection;
    
    /**
     * Creates a new AbstractDatabaseOperation with a specific database connection.
     * This is the preferred constructor for dependency injection.
     *
     * @param connection the database connection to use
     * @throws NullPointerException if connection is null
     */
    protected AbstractDatabaseOperation(AbstractDatabaseConnection connection) {
        this.connection = Objects.requireNonNull(connection, "Database connection cannot be null");
    }
    
    /**
     * Creates a new AbstractDatabaseOperation with the specified connection configuration.
     * This constructor uses the template method pattern to delegate connection creation
     * to the subclass.
     *
     * @param config the connection configuration
     * @throws SQLException if a database access error occurs
     */
    protected AbstractDatabaseOperation(ConnectionConfig config) throws SQLException {
        Objects.requireNonNull(config, "Connection configuration cannot be null");
        try {
            this.connection = createDatabaseConnection(config);
            log.debug("Created database operation with new connection for {}", config.getHost());
        } catch (SQLException e) {
            log.error("Failed to create database connection for operation", e);
            throw new SQLException("Failed to create database connection for operation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates a new AbstractDatabaseOperation with basic connection parameters.
     *
     * @param host database host
     * @param username database username
     * @param password database password
     * @throws SQLException if a database access error occurs
     */
    protected AbstractDatabaseOperation(String host, String username, String password) throws SQLException {
        this(createBasicConfig(host, username, password));
    }
    
    /**
     * Creates a basic connection configuration from parameters.
     *
     * @param host database host
     * @param username database username
     * @param password database password
     * @return a connection configuration
     */
    private static ConnectionConfig createBasicConfig(String host, String username, String password) {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(host);
        config.setUsername(username);
        config.setPassword(password);
        return config;
    }
    
    /**
     * Creates the appropriate database connection implementation.
     * This is a factory method that subclasses must implement to provide
     * the specific database connection type.
     *
     * @param config the connection configuration
     * @return a database connection implementation
     * @throws SQLException if a database access error occurs
     */
    protected abstract AbstractDatabaseConnection createDatabaseConnection(ConnectionConfig config) throws SQLException;
    
    /**
     * Executes a database operation within a single connection.
     * The connection is automatically closed when the operation completes.
     *
     * @param <T> the return type of the operation
     * @param callback the operation to execute
     * @return the result of the operation
     * @throws SQLException if a database access error occurs
     */
    public <T> T execute(ConnectionCallback<T> callback) throws SQLException {
        log.debug("Executing database operation");
        try (Connection conn = connection.getConnection()) {
            return callback.execute(conn);
        } catch (SQLException e) {
            log.error("Database operation failed", e);
            throw new SQLException("Database operation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes a database operation with transaction support.
     * The transaction is committed if the operation completes successfully,
     * or rolled back if an exception occurs.
     *
     * @param <T> the return type of the operation
     * @param callback the operation to execute
     * @return the result of the operation
     * @throws SQLException if a database access error occurs
     */
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
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warn("Transaction rolled back due to exception", e);
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction", rollbackEx);
                }
            }
            log.error("Transaction failed", e);
            throw new SQLException("Transaction failed: " + e.getMessage(), e);
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
    
    /**
     * Functional interface for database operations.
     *
     * @param <T> the return type of the operation
     */
    @FunctionalInterface
    public interface ConnectionCallback<T> {
        /**
         * Executes the operation with the given connection.
         *
         * @param connection the database connection
         * @return the result of the operation
         * @throws SQLException if a database access error occurs
         */
        T execute(Connection connection) throws SQLException;
    }
}