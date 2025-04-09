package com.example.shelldemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.example.shelldemo.model.AbstractConfigReader;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Implementation of ConfigReader for YAML configuration files.
 */
public class YamlConfigReader extends AbstractConfigReader {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public Map<String, Object> readConfig(String filePath) throws IOException {
        logReadStart(filePath, null);
        File file = validateAndGetFile(filePath);
        return yamlMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
    }

    @Override
    public <T> T readConfig(String filePath, Class<T> valueType) throws IOException {
        logReadStart(filePath, valueType);
        File file = validateAndGetFile(filePath);
        return yamlMapper.readValue(file, valueType);
    }

    public <T> T readConfig(String filePath, TypeReference<T> valueType) throws IOException {
        logReadStart(filePath, null);
        File file = validateAndGetFile(filePath);
        return yamlMapper.readValue(file, valueType);
    }

    public <T> T convertValue(Object fromValue, TypeReference<T> toValueType) {
        return yamlMapper.convertValue(fromValue, toValueType);
    }
} 