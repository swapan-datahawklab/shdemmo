package com.example.shelldemo.connection;

import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import com.example.shelldemo.exception.DriverLoadException;

/**
 * Handles JDBC driver loading using Java's Service Provider Interface (SPI).
 * Responsible for dynamically loading and registering database drivers at runtime.
 */
public class JdbcDriverLoader {
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(JdbcDriverLoader.class);

    /**
     * Loads and registers a JDBC driver from the specified JAR file path.
     *
     * @param driverPath Path to the JDBC driver JAR file
     * @throws DriverLoadException if driver loading fails
     */
    public void loadDriver(String driverPath) throws DriverLoadException {
        try {
            URL[] urls = { URI.create("file:" + driverPath).toURL() };
            URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());
            ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class, classLoader);
            
            boolean driverFound = false;
            for (Driver driver : drivers) {
                DriverManager.registerDriver(new JdbcDriverWrapper(driver));
                logger.info("Registered JDBC driver: {}", driver.getClass().getName());
                driverFound = true;
            }
            
            if (!driverFound) {
                throw new DriverLoadException("No JDBC drivers found in: " + driverPath);
            }
        } catch (Exception e) {
            throw new DriverLoadException("Failed to load JDBC driver from path: " + driverPath, e);
        }
    }

    private static class JdbcDriverWrapper implements Driver {
        private final Driver delegate;

        JdbcDriverWrapper(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
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
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}
