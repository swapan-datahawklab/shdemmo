package com.example.shelldemo.connection;

import java.sql.*;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom JDBC driver implementation that wraps an existing driver.
 * This allows for dynamic loading of JDBC drivers at runtime.
 */
public class CustomDriver implements Driver {
    private static final Logger logger = LoggerFactory.getLogger(CustomDriver.class);
    private final Driver delegate;

    public CustomDriver(Driver delegate) {
        if (delegate == null) {
            String errorMessage = "Delegate driver cannot be null";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        this.delegate = delegate;
        logger.info("Created CustomDriver wrapping {}", delegate.getClass().getName());
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        logger.debug("Attempting to connect to {} using delegate driver", url);
        try {
            Connection conn = delegate.connect(url, info);
            if (conn != null) {
                logger.info("Successfully established connection to {}", url);
            } else {
                logger.debug("Connection attempt returned null for {} - URL not recognized by driver", url);
            }
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to establish connection to {}: {}", url, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        try {
            boolean accepts = delegate.acceptsURL(url);
            logger.trace("URL {} {} accepted by delegate driver", url, accepts ? "was" : "was not");
            return accepts;
        } catch (SQLException e) {
            logger.error("Error checking URL acceptance for {}: {}", url, e.getMessage());
            throw e;
        }
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        logger.trace("Getting property info for URL: {}", url);
        try {
            return delegate.getPropertyInfo(url, info);
        } catch (SQLException e) {
            logger.error("Error getting property info for {}: {}", url, e.getMessage());
            throw e;
        }
    }

    @Override
    public int getMajorVersion() {
        int version = delegate.getMajorVersion();
        logger.trace("Retrieved major version: {}", version);
        return version;
    }

    @Override
    public int getMinorVersion() {
        int version = delegate.getMinorVersion();
        logger.trace("Retrieved minor version: {}", version);
        return version;
    }

    @Override
    public boolean jdbcCompliant() {
        boolean compliant = delegate.jdbcCompliant();
        logger.trace("Delegate driver JDBC compliance: {}", compliant);
        return compliant;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        logger.trace("Getting parent logger from delegate driver");
        try {
            return delegate.getParentLogger();
        } catch (SQLFeatureNotSupportedException e) {
            logger.debug("Parent logger not supported by delegate driver: {}", e.getMessage());
            throw e;
        }
    }
} 