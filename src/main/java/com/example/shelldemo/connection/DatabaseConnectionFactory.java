package com.example.shelldemo.connection;

import com.example.shelldemo.config.DatabaseProperties;
import com.example.shelldemo.model.entity.config.ConnectionConfig;
import com.example.shelldemo.exception.DatabaseConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating database connections.
 * Supports dynamic loading of JDBC drivers and manages direct JDBC connections.
 */
public class DatabaseConnectionFactory {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionFactory.class);
    private final DatabaseProperties properties;
    private final Map<String, Driver> drivers;

    public DatabaseConnectionFactory(DatabaseProperties properties) {
        logger.debug("Initializing DatabaseConnectionFactory");
        this.properties = properties;
        this.drivers = new ConcurrentHashMap<>();
        loadDrivers();
        logger.info("DatabaseConnectionFactory initialized with {} drivers", drivers.size());
    }

    private void loadDrivers() {
        logger.debug("Loading JDBC drivers using ServiceLoader");
        // Load drivers using ServiceLoader
        ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
        for (Driver driver : loadedDrivers) {
            String name = driver.getClass().getName().toLowerCase();
            drivers.put(name, driver);
            logger.debug("Loaded JDBC driver: {}", name);
        }
        logger.info("Successfully loaded {} JDBC drivers", drivers.size());
    }

    public Connection getConnection(ConnectionConfig config) throws SQLException {
        logger.debug("Getting connection for database type: {}, host: {}:{}", 
            config.getDbType(), config.getHost(), config.getPort());
            
        try {
            String url = getConnectionUrl(config, config.getDbType());
            logger.debug("Generated connection URL: {}", url);

            Properties props = new Properties();
            props.setProperty("user", config.getUsername());
            props.setProperty("password", "********"); // Masked for logging
            
            // Add any database-specific properties
            Map<String, String> dbProps = properties.getConnectionProperties(config.getDbType());
            props.putAll(dbProps);
            logger.trace("Connection properties: {}", props);

            logger.debug("Attempting to establish database connection");
            Connection conn = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            logger.info("Successfully established connection to {}:{}", config.getHost(), config.getPort());
            return conn;
        } catch (SQLException e) {
            String errorMessage = String.format("Failed to get connection for %s database at %s:%d",
                config.getDbType(), config.getHost(), config.getPort());
            logger.error(errorMessage, e);
            throw new DatabaseConnectionException(errorMessage, e, 
                DatabaseConnectionException.ERROR_CODE_CONNECTION_FAILED,
                String.format("dbType=%s, host=%s, port=%d", 
                    config.getDbType(), config.getHost(), config.getPort()));
        }
    }

    private String getConnectionUrl(ConnectionConfig config, String dbType) {
        logger.debug("Generating connection URL for database type: {}", dbType);
        String urlTemplate = properties.getUrlTemplate(dbType);
        
        // For Oracle, check if we should use LDAP connection
        if (dbType.equals("oracle") && "ldap".equals(config.getConnectionType())) {
            logger.debug("Using LDAP connection template for Oracle");
            urlTemplate = properties.getProperty("databases.types.oracle.ldapTemplate");
        }
        
        if (urlTemplate == null) {
            String errorMessage = "No URL template found for database type: " + dbType;
            logger.error(errorMessage);
            throw new DatabaseConnectionException(errorMessage, 
                DatabaseConnectionException.ERROR_CODE_INVALID_CONFIG);
        }

        String url = String.format(urlTemplate, config.getHost(), config.getPort(), config.getServiceName());
        logger.debug("Generated connection URL: {}", url);
        return url;
    }
} 