package com.example.shelldemo.connection;

import java.util.Properties;

/**
 * Generic interface for database connection string generation.
 * Each database type will implement this interface to provide
 * its specific connection string format and properties.
 */
public interface DatabaseConnectionStringGenerator {
    /**
     * Generates a connection string for the database
     * @param config connection configuration containing host, port, serviceName etc.
     * @return formatted connection string
     */
    String generateConnectionString(ConnectionConfig config);

    /**
     * Generates connection properties required for the connection
     * @param config connection configuration containing authentication details
     * @return Properties object with required connection properties
     */
    Properties generateConnectionProperties(ConnectionConfig config);

    /**
     * @return the database type this generator handles (oracle, postgresql, mysql, sqlserver)
     */
    String getDatabaseType();
} 