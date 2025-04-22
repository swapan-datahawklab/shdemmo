package com.example.shelldemo.connection;

import java.sql.Connection;
import java.sql.SQLException;

import com.example.shelldemo.exception.DatabaseConnectionException;

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
     * Creates a new database connection using the provided configuration.
     */
    Connection getConnection(ConnectionConfig config) throws SQLException;

    /**
     * Initializes any resources needed for connection management.
     */
    void initialize() throws DatabaseConnectionException;

    /**
     * Releases any resources held by the connection manager.
     */
    void shutdown();
} 