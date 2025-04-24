package com.example.shelldemo.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseTypeConfig {
    private int defaultPort;
    private Map<String, Map<String, String>> templates;
    private Map<String, String> properties;

    public int getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    public Map<String, Map<String, String>> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, Map<String, String>> templates) {
        this.templates = templates;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Gets a template by category and name
     * @param category Template category (e.g., "jdbc", "sql")
     * @param name Template name within the category
     * @return The template string, or null if not found
     */
    public String getTemplate(String category, String name) {
        if (templates == null || !templates.containsKey(category)) {
            return null;
        }

        Map<String, String> categoryTemplates = templates.get(category);
        String template = categoryTemplates.get(name);
        
        // If template not found, try the default
        if (template == null && categoryTemplates.containsKey("default")) {
            return categoryTemplates.get("default");
        } else if (template == null && categoryTemplates.containsKey("defaultTemplate")) {
            return categoryTemplates.get("defaultTemplate");
        }
        
        return template;
    }
}