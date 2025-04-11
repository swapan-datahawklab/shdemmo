package com.example.shelldemo.model.contract;

import com.example.shelldemo.exception.ConfigurationException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Map;

/**
 * Interface for reading configuration files.
 * Implementations should handle specific configuration formats (YAML, JSON, Properties, etc.).
 * 
 * Logging requirements for implementations:
 * 1. Use SLF4J for logging
 * 2. Log levels:
 *    - TRACE: Detailed operation information and value conversions
 *    - DEBUG: Configuration file operations and validation
 *    - INFO: Successful configuration loading
 *    - ERROR: Configuration errors with context
 * 3. Include file paths and types in logs
 * 4. Mask sensitive configuration values
 * 5. Use proper error codes from ConfigurationException
 */
public interface ConfigReader {
    /**
     * Reads a configuration file and returns its contents as a Map.
     * 
     * Logging requirements:
     * - DEBUG: Log read attempt with file path
     * - INFO: Log successful read with entry count
     * - ERROR: Log read failures with proper error code
     *
     * @param path Path to the configuration file
     * @return Map containing the configuration
     * @throws ConfigurationException if the file cannot be read or parsed
     */
    default Map<String, Object> readConfig(String path) throws IOException {
        return readConfig(path, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Reads a configuration file and maps it to a specific class.
     * 
     * Logging requirements:
     * - DEBUG: Log read attempt with file path and target class
     * - TRACE: Log conversion details
     * - INFO: Log successful mapping
     * - ERROR: Log mapping failures with proper error code
     *
     * @param path Path to the configuration file
     * @param valueType Class to map the configuration to
     * @param <T> Type of the configuration class
     * @return Instance of the configuration class
     * @throws ConfigurationException if the file cannot be read or parsed
     */
    default <T> T readConfig(String path, Class<T> valueType) throws IOException {
        return convertValue(readConfig(path), new TypeReference<T>() {});
    }

    /**
     * Reads a configuration file and maps it to a specific type reference.
     * 
     * Logging requirements:
     * - DEBUG: Log read attempt with file path and type reference
     * - TRACE: Log conversion details
     * - INFO: Log successful mapping
     * - ERROR: Log mapping failures with proper error code
     *
     * @param path Path to the configuration file
     * @param typeRef Type reference to map the configuration to
     * @param <T> Type of the configuration
     * @return Instance of the configuration
     * @throws ConfigurationException if the file cannot be read or parsed
     */
    <T> T readConfig(String path, TypeReference<T> typeRef) throws IOException;

    /**
     * Converts a value to a specific type.
     * 
     * Logging requirements:
     * - TRACE: Log conversion details including source and target types
     * - DEBUG: Log successful conversion
     * - ERROR: Log conversion failures with proper error code
     *
     * @param value Value to convert
     * @param typeRef Type reference to convert to
     * @param <T> Target type
     * @return Converted value
     * @throws ConfigurationException if the conversion fails
     */
    <T> T convertValue(Object value, TypeReference<T> typeRef);
} 