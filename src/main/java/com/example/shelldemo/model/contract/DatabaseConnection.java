package com.example.shelldemo.model.contract;

import com.example.shelldemo.model.entity.config.ConnectionConfig;
import com.example.shelldemo.exception.DatabaseConnectionException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for database connections.
 * Defines the contract for creating and managing database connections.
 * 
 * Implementations must follow these logging requirements:
 * 1. Use SLF4J for logging
 * 2. Log levels:
 *    - TRACE: Connection details (excluding sensitive data)
 *    - DEBUG: Connection lifecycle events
 *    - INFO: Important state changes
 *    - ERROR: Connection failures with context
 * 3. Mask sensitive information (passwords, credentials)
 * 4. Include operation context in logs
 * 5. Use proper error codes from DatabaseConnectionException
 */
public interface DatabaseConnection {
    /**
     * Creates a new database connection.
     * 
     * Logging requirements:
     * - DEBUG: Log connection attempt with database type and URL (mask credentials)
     * - INFO: Log successful connection
     * - ERROR: Log failed connection with error context
     *
     * @return A new database connection
     * @throws DatabaseConnectionException if connection creation fails
     */
    Connection createConnection() throws SQLException;

    /**
     * Creates a new database connection using the provided configuration.
     * 
     * Logging requirements:
     * - TRACE: Log connection properties (mask sensitive data)
     * - DEBUG: Log connection attempt with database type and URL
     * - INFO: Log successful connection
     * - ERROR: Log failed connection with error context and proper error code
     *
     * @param config Connection configuration
     * @return A new database connection
     * @throws DatabaseConnectionException if connection creation fails
     */
    Connection createConnection(ConnectionConfig config) throws SQLException;

    /**
     * Closes the database connection if it is open.
     * 
     * Logging requirements:
     * - DEBUG: Log connection close attempt
     * - INFO: Log successful connection close
     * - ERROR: Log failed close operation with error context
     *
     * @throws DatabaseConnectionException if closing the connection fails
     */
    void close() throws SQLException;
} 