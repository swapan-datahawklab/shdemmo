package com.example.shelldemo.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabasesConfig {
    private Map<String, DatabaseTypeConfig> types;

    public Map<String, DatabaseTypeConfig> getTypes() {
        return types;
    }

    public void setTypes(Map<String, DatabaseTypeConfig> types) {
        this.types = types;
    }
}