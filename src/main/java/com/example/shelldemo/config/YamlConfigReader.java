package com.example.shelldemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.example.shelldemo.model.base.AbstractConfigReader;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Collections;

/**
 * Implementation of ConfigReader for YAML configuration files.
 * Loads and caches configuration at startup for better performance.
 */
public class YamlConfigReader extends AbstractConfigReader {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, Object> cachedConfig;
    private final String configFilePath;

    /**
     * Creates a new YamlConfigReader and loads the configuration from the specified file.
     *
     * @param configFilePath Path to the YAML configuration file
     * @throws IOException If there's an error reading the configuration file
     */
    public YamlConfigReader(String configFilePath) throws IOException {
        this.configFilePath = configFilePath;
        this.cachedConfig = loadConfig();
    }

    /**
     * Loads the configuration from the YAML file.
     *
     * @return The loaded configuration as a Map
     * @throws IOException If there's an error reading the file
     */
    private Map<String, Object> loadConfig() throws IOException {
        logReadStart(configFilePath, null);
        File file = validateAndGetFile(configFilePath);
        return yamlMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
    }

    @Override
    public Map<String, Object> readConfig(String filePath) {
        if (!filePath.equals(configFilePath)) {
            throw new IllegalArgumentException("This reader is configured to use: " + configFilePath);
        }
        return Collections.unmodifiableMap(cachedConfig);
    }

    @Override
    public <T> T readConfig(String filePath, Class<T> valueType) {
        if (!filePath.equals(configFilePath)) {
            throw new IllegalArgumentException("This reader is configured to use: " + configFilePath);
        }
        return yamlMapper.convertValue(cachedConfig, valueType);
    }

    public <T> T readConfig(String filePath, TypeReference<T> valueType) {
        if (!filePath.equals(configFilePath)) {
            throw new IllegalArgumentException("This reader is configured to use: " + configFilePath);
        }
        return yamlMapper.convertValue(cachedConfig, valueType);
    }

    public <T> T convertValue(Object fromValue, TypeReference<T> toValueType) {
        return yamlMapper.convertValue(fromValue, toValueType);
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
        return value != null ? yamlMapper.convertValue(value, valueType) : null;
    }
} 