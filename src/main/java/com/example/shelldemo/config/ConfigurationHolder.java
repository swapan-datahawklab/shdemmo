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

    private ConfigurationHolder() throws IOException {
        logger.debug("Initializing ConfigurationHolder with config path: {}", CONFIG_PATH);
        this.configReader = new YamlConfigReader(CONFIG_PATH);
        
        logger.debug("Loading database types configuration");
        this.databaseTypes = configReader.readConfig("databases.types", 
            new TypeReference<Map<String, Map<String, Object>>>() {});
        logger.info("Loaded {} database types", databaseTypes.size());
        
        logger.debug("Loading database instances configuration");
        this.databaseInstances = configReader.readConfig("databases.instances", 
            new TypeReference<List<Map<String, String>>>() {});
        logger.info("Loaded {} database instances", databaseInstances.size());
        
        logger.debug("Loading LDAP configuration");
        this.ldapConfig = configReader.readConfig("ldap", 
            new TypeReference<Map<String, Object>>() {});
        logger.info("Loaded LDAP configuration with {} entries", ldapConfig.size());
        
        this.runtimeProperties = new ConcurrentHashMap<>();
        logger.info("ConfigurationHolder initialization complete");
    }

    public static synchronized ConfigurationHolder getInstance() throws IOException {
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
} 