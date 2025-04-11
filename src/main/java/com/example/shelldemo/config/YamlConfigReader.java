package com.example.shelldemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.example.shelldemo.model.contract.AbstractConfigReader;
import com.example.shelldemo.exception.ConfigurationException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Implementation of ConfigReader for YAML configuration files.
 * Loads and caches configuration at startup for better performance.
 */
public class YamlConfigReader extends AbstractConfigReader {
    private static final Logger logger = LoggerFactory.getLogger(YamlConfigReader.class);
    private final Map<String, Object> cachedConfig;
    private final String configFilePath;

    /**
     * Creates a new YamlConfigReader and loads the configuration from the specified file.
     *
     * @param configFilePath Path to the YAML configuration file
     * @throws IOException If there's an error reading the configuration file
     */
    public YamlConfigReader(String configFilePath) throws IOException {
        super(new ObjectMapper(new YAMLFactory()));
        logger.debug("Initializing YamlConfigReader with config path: {}", configFilePath);
        this.configFilePath = configFilePath;
        this.cachedConfig = loadConfig();
        logger.info("YamlConfigReader initialized successfully with {} configuration entries", cachedConfig.size());
    }

    /**
     * Loads the configuration from the YAML file.
     *
     * @return The loaded configuration as a Map
     * @throws IOException If there's an error reading the file
     */
    private Map<String, Object> loadConfig() throws IOException {
        logger.debug("Loading configuration from file: {}", configFilePath);
        try {
            File file = validateAndGetFile(configFilePath);
            Map<String, Object> config = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            logger.debug("Successfully loaded {} configuration entries", config.size());
            return config;
        } catch (IOException e) {
            String errorMessage = "Failed to load configuration from " + configFilePath;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    /**
     * Gets a specific configuration section by key.
     *
     * @param key The configuration key to retrieve
     * @return The configuration value for the specified key
     */
    public Object getConfigSection(String key) {
        logger.trace("Retrieving configuration section for key: {}", key);
        Object value = cachedConfig.get(key);
        if (value == null) {
            logger.debug("No configuration found for key: {}", key);
        }
        return value;
    }

    /**
     * Gets a specific configuration section by key and converts it to the specified type.
     *
     * @param key The configuration key to retrieve
     * @param valueType The type to convert the configuration to
     * @return The configuration value converted to the specified type
     */
    public <T> T getConfigSection(String key, Class<T> valueType) {
        logger.trace("Retrieving and converting configuration section for key: {} to type: {}", 
            key, valueType.getSimpleName());
        Object value = cachedConfig.get(key);
        if (value == null) {
            logger.debug("No configuration found for key: {}", key);
            return null;
        }
        try {
            T converted = objectMapper.convertValue(value, valueType);
            logger.trace("Successfully converted configuration value to {}", valueType.getSimpleName());
            return converted;
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format("Failed to convert configuration value for key '%s' to type %s", 
                key, valueType.getSimpleName());
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T readConfig(String path, TypeReference<T> typeRef) throws IOException {
        logger.debug("Reading configuration for path: {}", path);
        if (!path.equals(configFilePath)) {
            String errorMessage = "This reader is configured to use: " + configFilePath;
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            T config = objectMapper.convertValue(cachedConfig, typeRef);
            logger.debug("Successfully read configuration for path: {}", path);
            return config;
        } catch (IllegalArgumentException e) {
            String errorMessage = "Failed to convert configuration for path: " + path;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeRef) {
        logger.trace("Converting configuration value to type: {}", typeRef.getType().getTypeName());
        try {
            T converted = objectMapper.convertValue(value, typeRef);
            logger.trace("Successfully converted configuration value");
            return converted;
        } catch (IllegalArgumentException e) {
            String errorMessage = "Failed to convert configuration value";
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }
} 