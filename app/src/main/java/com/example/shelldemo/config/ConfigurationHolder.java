package com.example.shelldemo.config;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton configuration holder that loads and caches application configuration at startup.
 */
public class ConfigurationHolder {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationHolder.class);
    private static final String CONFIG_PATH = "application.yaml";
    private static ConfigurationHolder instance;
    
    private final YamlConfigReader configReader;
    private final Map<String, Map<String, Object>> databaseTypes;
    private final List<Map<String, String>> databaseInstances;
    private final Map<String, Object> ldapConfig;
    private final Map<String, String> runtimeProperties;

    private ConfigurationHolder() {
        logger.debug("Initializing ConfigurationHolder");
        this.runtimeProperties = new ConcurrentHashMap<>();
        try {
            this.configReader = new YamlConfigReader(CONFIG_PATH);
            logger.debug("Loading database types from configuration");
            this.databaseTypes = configReader.readConfig("databases.types", 
                new TypeReference<Map<String, Map<String, Object>>>() {});
            logger.debug("Found database types: {}", databaseTypes.keySet());
            logger.debug("Loading database instances from configuration");
            this.databaseInstances = configReader.readConfig("databases.instances", 
                new TypeReference<List<Map<String, String>>>() {});
            logger.debug("Found {} database instances", databaseInstances.size());
            logger.debug("Loading LDAP configuration");
            this.ldapConfig = configReader.readConfig("ldap", 
                new TypeReference<Map<String, Object>>() {});
            logger.debug("LDAP configuration loaded with {} entries", ldapConfig.size());
            logger.info("ConfigurationHolder initialized successfully");
        } catch (IOException e) {
            String errorMessage = "Failed to initialize configuration";
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    public static synchronized ConfigurationHolder getInstance() {
        if (instance == null) {
            logger.debug("Creating new ConfigurationHolder instance");
            instance = new ConfigurationHolder();
            logger.info("ConfigurationHolder instance created successfully");
        }
        return instance;
    }

    public Map<String, Map<String, Object>> getDatabaseTypes() {
        logger.trace("Retrieving database types configuration");
        return databaseTypes;
    }

    public List<Map<String, String>> getDatabaseInstances() {
        logger.trace("Retrieving database instances configuration");
        return databaseInstances;
    }

    public Map<String, Object> getLdapConfig() {
        logger.trace("Retrieving LDAP configuration");
        return ldapConfig;
    }

    public void setRuntimeProperty(String key, String value) {
        logger.debug("Setting runtime property: {} = {}", key, 
            key.toLowerCase().contains("password") ? "********" : value);
        runtimeProperties.put(key, value);
    }

    public String getRuntimeProperty(String key) {
        String value = runtimeProperties.get(key);
        logger.trace("Retrieved runtime property: {} = {}", key, 
            key.toLowerCase().contains("password") ? "********" : value);
        return value;
    }

    public Map<String, String> getRuntimeProperties() {
        logger.trace("Creating copy of runtime properties");
        return new ConcurrentHashMap<>(runtimeProperties);
    }

    /**
     * Checks if a database type is valid.
     *
     * @param dbType database type to check
     * @return true if valid, false otherwise
     */
    public boolean isValidDbType(String dbType) {
        boolean isValid = dbType != null && databaseTypes.containsKey(dbType.toLowerCase());
        logger.trace("Database type {} is {}", dbType, isValid ? "valid" : "invalid");
        return isValid;
    }

    /**
     * Gets the default port for a database type.
     *
     * @param dbType database type
     * @return default port number
     * @throws ConfigurationException if database type is invalid or missing configuration
     */
    public int getDefaultPort(String dbType) {
        logger.debug("Getting default port for database type: {}", dbType);
        if (!isValidDbType(dbType)) {
            String errorMessage = "Invalid database type: " + dbType;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
        }
        
        Map<String, Object> typeConfig = databaseTypes.get(dbType.toLowerCase());
        Object defaultPort = typeConfig.get("defaultPort");
        if (defaultPort == null) {
            String errorMessage = "No default port configured for database type: " + dbType;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
        }
        
        int port = ((Number) defaultPort).intValue();
        logger.debug("Retrieved default port {} for database type {}", port, dbType);
        return port;
    }
} 