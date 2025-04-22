package com.example.shelldemo.connection.postgresql;

import com.example.shelldemo.connection.ConnectionConfig;
import com.example.shelldemo.connection.DatabaseConnectionStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;

/**
 * PostgreSQL implementation of DatabaseConnectionStringGenerator.
 */
public class PostgresConnectionStringGenerator implements DatabaseConnectionStringGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PostgresConnectionStringGenerator.class);

    @Override
    public String generateConnectionString(ConnectionConfig config) {
        logger.debug("Generating PostgreSQL connection string for host: {}, port: {}, database: {}", 
            config.getHost(), config.getPort(), config.getServiceName());
            
        String connectionString = String.format("jdbc:postgresql://%s:%d/%s",
            config.getHost(), config.getPort(), config.getServiceName());
            
        logger.debug("Generated connection string: {}", connectionString);
        return connectionString;
    }

    @Override
    public Properties generateConnectionProperties(ConnectionConfig config) {
        // PostgreSQL uses standard username/password authentication
        // which is handled by the main connection properties
        return new Properties();
    }

    @Override
    public String getDatabaseType() {
        return "postgresql";
    }
} 