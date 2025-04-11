package com.example.shelldemo.model.contract;

import com.example.shelldemo.exception.ConfigurationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for configuration readers.
 * Provides common functionality for reading and converting configuration data.
 */
public abstract class AbstractConfigReader implements ConfigReader {
    private static final Logger logger = LoggerFactory.getLogger(AbstractConfigReader.class);
    protected final ObjectMapper objectMapper;

    protected AbstractConfigReader(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            String errorMessage = "ObjectMapper cannot be null";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        this.objectMapper = objectMapper;
        logger.debug("Initialized AbstractConfigReader with ObjectMapper: {}", objectMapper.getClass().getName());
    }

    @Override
    public <T> T readConfig(String path, TypeReference<T> typeRef) throws IOException {
        logger.debug("Reading configuration from path: {} with type: {}", path, typeRef.getType().getTypeName());
        Path configPath = Paths.get(path);
        
        if (!Files.exists(configPath)) {
            String errorMessage = "Configuration file not found: " + path;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }

        try {
            T config = objectMapper.readValue(configPath.toFile(), typeRef);
            logger.debug("Successfully read configuration from: {}", path);
            return config;
        } catch (IOException e) {
            String errorMessage = "Failed to read configuration file: " + path;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeRef) {
        logger.trace("Converting value of type {} to {}", 
            value != null ? value.getClass().getSimpleName() : "null", 
            typeRef.getType().getTypeName());
        try {
            T converted = objectMapper.convertValue(value, typeRef);
            logger.trace("Successfully converted value to {}", typeRef.getType().getTypeName());
            return converted;
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
        logger.trace("Validating configuration value '{}' of type {}", name, expectedType.getSimpleName());
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
        logger.trace("Successfully validated configuration value: {}", name);
    }

    protected void logReadStart(String path, String type) {
        logger.debug("Reading configuration from {}{}", path, type != null ? " as " + type : "");
    }

    protected File validateAndGetFile(String path) throws IOException {
        logger.debug("Validating configuration file: {}", path);
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
        
        logger.debug("Successfully validated configuration file: {}", path);
        return file;
    }
} 