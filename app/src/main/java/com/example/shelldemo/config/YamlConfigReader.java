package com.example.shelldemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Generic YAML configuration reader that loads and caches configuration at startup.
 */
class YamlConfigReader extends AbstractConfigReader {
    private static final Logger logger = LogManager.getLogger(YamlConfigReader.class);
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
        try {
            var inputStream = YamlConfigReader.class.getClassLoader().getResourceAsStream(configFilePath);
            if (inputStream == null) {
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFilePath);
            }
            if (inputStream == null) {
                File file = validateAndGetFile(configFilePath);
                inputStream = file.toURI().toURL().openStream();
            }
            
            Map<String, Object> config = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
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
        return cachedConfig.get(key);
    }

    /**
     * Gets a specific configuration section by key and converts it to the specified type.
     *
     * @param key The configuration key to retrieve
     * @param valueType The type to convert the configuration to
     * @return The configuration value converted to the specified type
     */
    public <T> T getConfigSection(String key, Class<T> valueType) {
        Object value = cachedConfig.get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, valueType);
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format("Failed to convert configuration value for key '%s' to type %s", 
                key, valueType.getSimpleName());
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T readConfig(String path, TypeReference<T> typeRef) throws IOException {
        try {
            String[] pathParts = path.split("\\.");
            Object currentValue = cachedConfig;
            
            for (String part : pathParts) {
                if (currentValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) currentValue;
                    currentValue = map.get(part);
                    if (currentValue == null) {
                        String errorMessage = "Configuration not found for path: " + path;
                        logger.error(errorMessage);
                        throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
                    }
                } else {
                    String errorMessage = "Invalid path: " + path;
                    logger.error(errorMessage);
                    throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
                }
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
} 