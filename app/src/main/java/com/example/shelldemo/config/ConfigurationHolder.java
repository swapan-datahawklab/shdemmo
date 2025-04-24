package com.example.shelldemo.config;

import com.example.shelldemo.config.model.ApplicationConfig;
import com.example.shelldemo.config.model.DatabaseTypeConfig;
import com.example.shelldemo.exception.ConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton configuration holder that loads and caches application configuration at startup.
 */
public class ConfigurationHolder {
    private static final Logger logger = LogManager.getLogger(ConfigurationHolder.class);
    private static final String CONFIG_PATH = "application.yaml";
    private static ConfigurationHolder instance;
    
    private final YamlConfigReader configReader;
    private final ApplicationConfig applicationConfig;
    private final Map<String, String> runtimeProperties;

    private ConfigurationHolder() {
        this.runtimeProperties = new ConcurrentHashMap<>();
        try {
            this.configReader = new YamlConfigReader(CONFIG_PATH);
            this.applicationConfig = configReader.getApplicationConfig();
            logger.info("ConfigurationHolder initialized successfully");
        } catch (IOException e) {
            String errorMessage = "Failed to initialize configuration";
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    public static synchronized ConfigurationHolder getInstance() {
        if (instance == null) {
            instance = new ConfigurationHolder();
        }
        return instance;
    }

    public Map<String, DatabaseTypeConfig> getDatabaseTypes() {
        return applicationConfig.getDatabases().getTypes();
    }

    public DatabaseTypeConfig getDatabaseConfig(String dbType) {
        if (!isValidDbType(dbType)) {
            String errorMessage = "Invalid database type: " + dbType;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
        }
        return applicationConfig.getDatabases().getTypes().get(dbType.toLowerCase());
    }

    public String getJdbcClientTemplate(String dbType, String templateName) {
        return configReader.getDatabaseTemplate(dbType.toLowerCase(), "jdbc", templateName);
    }

    public String getSqlTemplate(String dbType, String templateName) {
        return configReader.getDatabaseTemplate(dbType.toLowerCase(), "sql", templateName);
    }

    public String getTemplate(String dbType, String category, String templateName) {
        return configReader.getDatabaseTemplate(dbType.toLowerCase(), category, templateName);
    }

    public void setRuntimeProperty(String key, String value) {
        runtimeProperties.put(key, value);
    }

    public String getRuntimeProperty(String key) {
        return runtimeProperties.get(key);
    }

    public Map<String, String> getRuntimeProperties() {
        return new ConcurrentHashMap<>(runtimeProperties);
    }

    public boolean isValidDbType(String dbType) {
        return dbType != null && 
               applicationConfig.getDatabases() != null && 
               applicationConfig.getDatabases().getTypes() != null &&
               applicationConfig.getDatabases().getTypes().containsKey(dbType.toLowerCase());
    }

    public int getDefaultPort(String dbType) {
        return getDatabaseConfig(dbType).getDefaultPort();
    }

    public Map<String, String> getDatabaseProperties(String dbType) {
        return getDatabaseConfig(dbType).getProperties();
    }
    
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }
}