package com.example.shelldemo.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Root configuration class that maps to the entire application.yaml file
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationConfig {
    private SpringConfig spring;
    private DatabasesConfig databases;
    private LoggingConfig logging;

    public SpringConfig getSpring() {
        return spring;
    }

    public void setSpring(SpringConfig spring) {
        this.spring = spring;
    }

    public DatabasesConfig getDatabases() {
        return databases;
    }

    public void setDatabases(DatabasesConfig databases) {
        this.databases = databases;
    }

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig logging) {
        this.logging = logging;
    }
}