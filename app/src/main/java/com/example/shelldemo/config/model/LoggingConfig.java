package com.example.shelldemo.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoggingConfig {
    private Map<String, String> level;
    private Map<String, String> pattern;

    public Map<String, String> getLevel() {
        return level;
    }

    public void setLevel(Map<String, String> level) {
        this.level = level;
    }

    public Map<String, String> getPattern() {
        return pattern;
    }

    public void setPattern(Map<String, String> pattern) {
        this.pattern = pattern;
    }
}