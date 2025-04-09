package com.example.shelldemo.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Abstract base class for configuration readers.
 * Provides common functionality and logging for configuration reading operations.
 */
public abstract class AbstractConfigReader implements ConfigReader {
    protected final Logger logger = LogManager.getLogger(getClass());

    /**
     * Validates the configuration file path and returns a File object.
     *
     * @param filePath Path to the configuration file
     * @return File object representing the configuration file
     * @throws IOException if the file is invalid or cannot be accessed
     */
    protected File validateAndGetFile(String filePath) throws IOException {
        logger.debug("Validating configuration file: {}", filePath);
        File file = new File(filePath);
        
        if (!file.exists()) {
            throw new IOException("Configuration file does not exist: " + filePath);
        }
        if (!file.isFile()) {
            throw new IOException("Path is not a file: " + filePath);
        }
        if (!file.canRead()) {
            throw new IOException("Cannot read configuration file: " + filePath);
        }
        
        return file;
    }

    /**
     * Logs the start of a configuration reading operation.
     *
     * @param filePath Path to the configuration file
     * @param valueType Type of object being read (null if reading as Map)
     */
    protected void logReadStart(String filePath, Class<?> valueType) {
        if (valueType != null) {
            logger.debug("Reading configuration from: {} to type: {}", filePath, valueType.getName());
        } else {
            logger.debug("Reading configuration from: {} as Map", filePath);
        }
    }
} 