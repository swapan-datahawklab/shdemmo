package com.example.shelldemo.connection;

import java.sql.*;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Custom JDBC driver implementation that wraps an existing driver.
 * This allows for dynamic loading of JDBC drivers at runtime.
 */
public class CustomDriver implements Driver {
    private static final Logger logger = LogManager.getLogger(CustomDriver.class);
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
        try {
            Connection conn = delegate.connect(url, info);
            if (conn != null) {
                logger.info("Successfully established connection to {}", url);
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
            return delegate.acceptsURL(url);
        } catch (SQLException e) {
            logger.error("Error checking URL acceptance for {}: {}", url, e.getMessage());
            throw e;
        }
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        try {
            return delegate.getPropertyInfo(url, info);
        } catch (SQLException e) {
            logger.error("Error getting property info for {}: {}", url, e.getMessage());
            throw e;
        }
    }

    @Override
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return delegate.jdbcCompliant();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        try {
            return delegate.getParentLogger();
        } catch (SQLFeatureNotSupportedException e) {
            throw e;
        }
    }
} 