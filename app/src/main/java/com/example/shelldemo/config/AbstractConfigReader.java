package com.example.shelldemo.config;

import com.example.shelldemo.exception.ConfigurationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract base class for configuration readers.
 * Provides common functionality for reading and converting configuration data.
 */
abstract class AbstractConfigReader implements ConfigReader {
    private static final Logger logger = LogManager.getLogger(AbstractConfigReader.class);
    protected final ObjectMapper objectMapper;

    protected AbstractConfigReader(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            String errorMessage = "ObjectMapper cannot be null";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T readConfig(String path, TypeReference<T> typeRef) throws IOException {
        Path configPath = Paths.get(path);
        
        if (!Files.exists(configPath)) {
            String errorMessage = "Configuration file not found: " + path;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }

        try {
            return objectMapper.readValue(configPath.toFile(), typeRef);
        } catch (IOException e) {
            String errorMessage = "Failed to read configuration file: " + path;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeRef) {
        try {
            return objectMapper.convertValue(value, typeRef);
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format("Failed to convert value of type %s to %s",
                value != null ? value.getClass().getSimpleName() : "null",
                typeRef.getType().getTypeName());
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    /**
     * Validates that a configuration value exists and is of the expected type.
     *
     * @param value Value to validate
     * @param name Name of the configuration property
     * @param expectedType Expected type of the value
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateConfigValue(Object value, String name, Class<?> expectedType) {
        if (value == null) {
            String errorMessage = "Missing required configuration: " + name;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
        }
        if (!expectedType.isInstance(value)) {
            String errorMessage = String.format("Invalid type for %s. Expected %s, got %s",
                name, expectedType.getSimpleName(), value.getClass().getSimpleName());
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
        }
    }

    protected File validateAndGetFile(String path) throws IOException {
        File file = new File(path);
        
        if (!file.exists()) {
            String errorMessage = "Configuration file not found: " + path;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
        
        if (!file.canRead()) {
            String errorMessage = "Cannot read configuration file: " + path;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
        
        return file;
    }
} 