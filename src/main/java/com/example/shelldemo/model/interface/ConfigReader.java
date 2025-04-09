package com.example.shelldemo.model;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for reading configuration files.
 * Implementations should handle specific configuration formats (YAML, JSON, Properties, etc.).
 */
public interface ConfigReader {
    /**
     * Reads a configuration file and returns its contents as a Map.
     *
     * @param filePath Path to the configuration file
     * @return Map containing the configuration
     * @throws IOException if the file cannot be read or parsed
     */
    Map<String, Object> readConfig(String filePath) throws IOException;

    /**
     * Reads a configuration file and maps it to a specific class.
     *
     * @param filePath Path to the configuration file
     * @param valueType Class to map the configuration to
     * @param <T> Type of the configuration class
     * @return Instance of the configuration class
     * @throws IOException if the file cannot be read or parsed
     */
    <T> T readConfig(String filePath, Class<T> valueType) throws IOException;
} 