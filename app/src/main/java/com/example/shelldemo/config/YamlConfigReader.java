package com.example.shelldemo.config;

import com.example.shelldemo.config.model.ApplicationConfig;
import com.example.shelldemo.config.model.DatabaseTypeConfig;
import com.example.shelldemo.exception.ConfigurationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * Generic YAML configuration reader that loads and caches configuration at startup.
 */
class YamlConfigReader extends AbstractConfigReader {
    private static final Logger logger = LogManager.getLogger(YamlConfigReader.class);
    private final String configFilePath;
    private final ApplicationConfig applicationConfig;

    /**
     * Creates a new YamlConfigReader and loads the configuration from the specified file.
     *
     * @param configFilePath Path to the YAML configuration file
     * @throws IOException If there's an error reading the configuration file
     */
    public YamlConfigReader(String configFilePath) throws IOException {
        super(new ObjectMapper(new YAMLFactory()));
        this.configFilePath = configFilePath;
        this.applicationConfig = loadConfig();
        logger.info("YamlConfigReader initialized successfully");
    }

    /**
     * Loads the configuration from the YAML file into the ApplicationConfig POJO.
     *
     * @return The loaded configuration as ApplicationConfig
     * @throws IOException If there's an error reading the file
     */
    private ApplicationConfig loadConfig() throws IOException {
        try {
            InputStream inputStream = YamlConfigReader.class.getClassLoader().getResourceAsStream(configFilePath);
            if (inputStream == null) {
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFilePath);
            }
            if (inputStream == null) {
                File file = validateAndGetFile(configFilePath);
                inputStream = file.toURI().toURL().openStream();
            }
            
            return objectMapper.readValue(inputStream, ApplicationConfig.class);
        } catch (IOException e) {
            String errorMessage = "Failed to load configuration from " + configFilePath;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    /**
     * Gets the root application configuration.
     *
     * @return The ApplicationConfig object
     */
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    /**
     * Gets the database configuration for a specific database type.
     *
     * @param dbType The database type (e.g., "oracle", "mysql")
     * @return The DatabaseTypeConfig for the specified type
     * @throws ConfigurationException If the database type is not found
     */
    public DatabaseTypeConfig getDatabaseConfig(String dbType) {
        if (applicationConfig.getDatabases() == null || 
            applicationConfig.getDatabases().getTypes() == null ||
            !applicationConfig.getDatabases().getTypes().containsKey(dbType)) {
            
            String errorMessage = "Database configuration not found for type: " + dbType;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
        }
        
        return applicationConfig.getDatabases().getTypes().get(dbType);
    }

    /**
     * Gets a database template by type, category, and name.
     *
     * @param dbType The database type (e.g., "oracle", "mysql")
     * @param category The template category (e.g., "jdbc", "sql")
     * @param templateName The template name within the category
     * @return The template string
     * @throws ConfigurationException If the template is not found
     */
    public String getDatabaseTemplate(String dbType, String category, String templateName) {
        DatabaseTypeConfig dbConfig = getDatabaseConfig(dbType);
        String template = dbConfig.getTemplate(category, templateName);
        
        if (template == null) {
            String errorMessage = String.format(
                "Template not found for database: %s, category: %s, name: %s",
                dbType, category, templateName);
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
        }
        
        return template;
    }
    
    @Override
    public <T> T readConfig(String path, TypeReference<T> typeRef) throws IOException {
        // This is a legacy method, but we'll keep it for backward compatibility
        try {
            String[] pathParts = path.split("\\.");
            Object currentValue = applicationConfig;
            
            for (String part : pathParts) {
                if (currentValue == null) {
                    String errorMessage = "Configuration not found for path: " + path;
                    logger.error(errorMessage);
                    throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
                }
                
                // Use reflection to navigate the path
                String getterName = "get" + part.substring(0, 1).toUpperCase() + part.substring(1);
                currentValue = getPropertyValue(currentValue, getterName, path, part);
            }
            
            return objectMapper.convertValue(currentValue, typeRef);
        } catch (IllegalArgumentException e) {
            String errorMessage = "Failed to convert configuration for path: " + path;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeRef) {
        try {
            return objectMapper.convertValue(value, typeRef);
        } catch (IllegalArgumentException e) {
            String errorMessage = "Failed to convert configuration value";
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    /**
     * Retrieves a property value using reflection.
     *
     * @param object The object to get the property from
     * @param getterName The name of the getter method
     * @param fullPath The full path being processed (for error reporting)
     * @param pathPart The current part of the path (for error reporting)
     * @return The property value
     * @throws ConfigurationException If the property cannot be accessed
     */
    private Object getPropertyValue(Object object, String getterName, String fullPath, String pathPart) {
        try {
            java.lang.reflect.Method getter = object.getClass().getMethod(getterName);
            return getter.invoke(object);
        } catch (Exception e) {
            String errorMessage = "Invalid path or missing property: " + fullPath + " at part: " + pathPart;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
        }
    }
}