package com.example.shelldemo.config;

import com.example.shelldemo.exception.ConfigurationException;
import com.example.shelldemo.exception.DatabaseTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages database configuration properties using the unified configuration holder.
 */
public class DatabaseProperties {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseProperties.class);
    private final ConfigurationHolder config;
    
    /**
     * Initializes database properties using the configuration holder.
     *
     * @throws ConfigurationException if configuration cannot be loaded
     */
    public DatabaseProperties() {
        logger.debug("Initializing DatabaseProperties");
        try {
            this.config = ConfigurationHolder.getInstance();
            logger.info("Successfully initialized DatabaseProperties with configuration holder");
        } catch (IOException e) {
            String errorMessage = "Failed to initialize database properties";
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    /**
     * Sets a runtime property.
     *
     * @param key property key
     * @param value property value
     */
    public void setRuntimeProperty(String key, String value) {
        logger.debug("Setting runtime property: {} = {}", key, key.toLowerCase().contains("password") ? "********" : value);
        config.setRuntimeProperty(key, value);
    }

    /**
     * Gets a property value.
     *
     * @param key property key
     * @return property value or null if not found
     */
    public String getProperty(String key) {
        String value = config.getRuntimeProperty(key);
        logger.trace("Retrieved property: {} = {}", key, key.toLowerCase().contains("password") ? "********" : value);
        return value;
    }

    /**
     * Gets the URL template for a database type.
     *
     * @param dbType database type
     * @return URL template string
     * @throws DatabaseTypeException if database type is invalid
     */
    public String getUrlTemplate(String dbType) {
        logger.debug("Getting URL template for database type: {}", dbType);
        validateDbType(dbType);
        String template = (String) config.getDatabaseTypes().get(dbType.toLowerCase()).get("urlTemplate");
        logger.debug("Retrieved URL template: {}", template);
        return template;
    }

    /**
     * Gets the default port for a database type.
     *
     * @param dbType database type
     * @return default port number
     * @throws DatabaseTypeException if database type is invalid
     */
    public int getDefaultPort(String dbType) {
        logger.debug("Getting default port for database type: {}", dbType);
        validateDbType(dbType);
        int port = ((Number) config.getDatabaseTypes().get(dbType.toLowerCase()).get("defaultPort")).intValue();
        logger.debug("Retrieved default port: {}", port);
        return port;
    }

    /**
     * Gets connection properties for a database type.
     *
     * @param dbType database type
     * @return map of connection properties
     * @throws DatabaseTypeException if database type is invalid
     */
    public Map<String, String> getConnectionProperties(String dbType) {
        logger.debug("Getting connection properties for database type: {}", dbType);
        validateDbType(dbType);
        
        Map<String, String> props = new ConcurrentHashMap<>();
        Map<String, Object> dbConfig = config.getDatabaseTypes().get(dbType.toLowerCase());
        
        // Add any additional connection properties from the configuration
        dbConfig.forEach((key, value) -> {
            if (!"urlTemplate".equals(key) && !"defaultPort".equals(key)) {
                props.put(key, value.toString());
                logger.trace("Added database property: {} = {}", key, 
                    key.toLowerCase().contains("password") ? "********" : value);
            }
        });
        
        // Runtime properties override configuration properties
        config.getRuntimeProperties().forEach((key, value) -> {
            if (key.startsWith("db." + dbType + ".")) {
                String propKey = key.substring(("db." + dbType + ".").length());
                props.put(propKey, value);
                logger.trace("Added runtime property: {} = {}", propKey, 
                    propKey.toLowerCase().contains("password") ? "********" : value);
            }
        });
        
        logger.debug("Retrieved {} connection properties for {}", props.size(), dbType);
        return props;
    }

    /**
     * Checks if a database type is valid.
     *
     * @param dbType database type to check
     * @return true if valid, false otherwise
     */
    public boolean isValidDbType(String dbType) {
        boolean isValid = dbType != null && config.getDatabaseTypes().containsKey(dbType.toLowerCase());
        logger.trace("Database type {} is {}", dbType, isValid ? "valid" : "invalid");
        return isValid;
    }

    /**
     * Validates a database type and throws an exception if invalid.
     *
     * @param dbType database type to validate
     * @throws DatabaseTypeException if database type is invalid
     */
    private void validateDbType(String dbType) {
        if (!isValidDbType(dbType)) {
            String errorMessage = "Invalid database type: " + dbType;
            logger.error(errorMessage);
            throw new DatabaseTypeException(errorMessage, DatabaseTypeException.ERROR_CODE_INVALID_TYPE);
        }
    }

    public Map<String, Object> getLdapConfig() {
        logger.debug("Retrieving LDAP configuration");
        Map<String, Object> ldapConfig = config.getLdapConfig();
        logger.debug("Retrieved LDAP configuration with {} entries", ldapConfig.size());
        return ldapConfig;
    }

    public List<Map<String, String>> getDatabaseInstances() {
        logger.debug("Retrieving database instances");
        List<Map<String, String>> instances = config.getDatabaseInstances();
        logger.debug("Retrieved {} database instances", instances.size());
        return instances;
    }
} 