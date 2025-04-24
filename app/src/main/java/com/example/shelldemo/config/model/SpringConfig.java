package com.example.shelldemo.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpringConfig {
    private ApplicationNameConfig application;

    public ApplicationNameConfig getApplication() {
        return application;
    }

    public void setApplication(ApplicationNameConfig application) {
        this.application = application;
    }

    public static class ApplicationNameConfig {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}