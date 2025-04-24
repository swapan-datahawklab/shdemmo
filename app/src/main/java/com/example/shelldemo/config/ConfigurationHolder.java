package com.example.shelldemo.config;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

/**
 * Singleton configuration holder that loads and caches application configuration at startup.
 */
public class ConfigurationHolder {
    private static final Logger logger = LogManager.getLogger(ConfigurationHolder.class);
    private static final String CONFIG_PATH = "application.yaml";
    private static ConfigurationHolder instance;
    
    private final YamlConfigReader configReader;
    private final Map<String, DatabaseTypeConfig> databaseTypes;
    private final Map<String, String> runtimeProperties;

    // New static class to represent the database type configuration structure
    public static class DatabaseTypeConfig {
        private int defaultPort;
        private Map<String, TemplateConfig> templates;
        private Map<String, String> properties;

        public int getDefaultPort() { return defaultPort; }
        public void setDefaultPort(int defaultPort) { this.defaultPort = defaultPort; }
        
        public Map<String, TemplateConfig> getTemplates() { return templates; }
        public void setTemplates(Map<String, TemplateConfig> templates) { this.templates = templates; }
        
        public Map<String, String> getProperties() { return properties; }
        public void setProperties(Map<String, String> properties) { this.properties = properties; }
    }

    // New static class to represent template configurations
    public static class TemplateConfig {
        private Map<String, String> jdbc; // This will hold dynamic JDBC templates
        private Map<String, String> sql;   // This will hold dynamic SQL templates
        private String defaultTemplate;     // Optional default template

        public Map<String, String> getJdbc() { return jdbc; }
        public void setJdbc(Map<String, String> jdbc) { this.jdbc = jdbc; }
        
        public Map<String, String> getSql() { return sql; }
        public void setSql(Map<String, String> sql) { this.sql = sql; }

        public String getDefaultTemplate() { return defaultTemplate; }
        public void setDefaultTemplate(String defaultTemplate) { this.defaultTemplate = defaultTemplate; }
    }

    private ConfigurationHolder() {
        this.runtimeProperties = new ConcurrentHashMap<>();
        try {
            this.configReader = new YamlConfigReader(CONFIG_PATH);
            this.databaseTypes = configReader.readConfig("databases.types", 
                new TypeReference<Map<String, DatabaseTypeConfig>>() {});
            logger.info("ConfigurationHolder initialized successfully");
        } catch (IOException e) {
            String errorMessage = "Failed to initialize configuration";
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    public static synchronized ConfigurationHolder getInstance() {
        if (instance == null) {
            instance = new ConfigurationHolder();
            logger.info("ConfigurationHolder instance created successfully");
        }
        return instance;
    }

    public Map<String, DatabaseTypeConfig> getDatabaseTypes() {
        return databaseTypes;
    }

    public DatabaseTypeConfig getDatabaseConfig(String dbType) {
        if (!isValidDbType(dbType)) {
            String errorMessage = "Invalid database type: " + dbType;
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
        }
        return databaseTypes.get(dbType.toLowerCase());
    }

    public String getJdbcClientTemplate(String dbType, String templateName) {
        DatabaseTypeConfig config = getDatabaseConfig(dbType);
        Map<String, TemplateConfig> templates = config.getTemplates();
        
        // First try to get from jdbc templates
        String template = null;
        if (templates.containsKey("jdbc")) {
            Map<String, String> jdbcTemplates = templates.get("jdbc").getJdbc();
            template = jdbcTemplates.get(templateName);
            if (template == null) {
                template = jdbcTemplates.get("default");
            }
        }
        
        if (template == null) {
            throw new ConfigurationException(
                "No JDBC client template found for " + dbType + " and name " + templateName,
                ConfigurationException.ERROR_CODE_MISSING_CONFIG
            );
        }
        return template;
    }

    public String getSqlTemplate(String dbType, String templateName) {
        DatabaseTypeConfig config = getDatabaseConfig(dbType);
        Map<String, TemplateConfig> templates = config.getTemplates();
        
        // Try to get from sql templates
        if (templates.containsKey("sql")) {
            Map<String, String> sqlTemplates = templates.get("sql").getSql();
            String template = sqlTemplates.get(templateName);
            if (template != null) {
                return template;
            }
        }
        
        throw new ConfigurationException(
            "No SQL template found for " + dbType + " and name " + templateName,
            ConfigurationException.ERROR_CODE_MISSING_CONFIG
        );
    }

    // Add a new generic method to get any template
    public String getTemplate(String dbType, String category, String templateName) {
        DatabaseTypeConfig config = getDatabaseConfig(dbType);
        Map<String, TemplateConfig> templates = config.getTemplates();
        
        if (!templates.containsKey(category)) {
            throw new ConfigurationException(
                "No template category found: " + category + " for database type: " + dbType,
                ConfigurationException.ERROR_CODE_MISSING_CONFIG
            );
        }
        
        // Use reflection to get the appropriate getter method
        try {
            String getterName = "get" + category.substring(0, 1).toUpperCase() + category.substring(1);
            Method getter = TemplateConfig.class.getMethod(getterName);
            @SuppressWarnings("unchecked")
            Map<String, String> categoryTemplates = (Map<String, String>) getter.invoke(templates.get(category));
            
            String template = categoryTemplates.get(templateName);
            if (template == null && templateName.equals("default")) {
                // If asking for default template and it's not found, throw specific error
                throw new ConfigurationException(
                    "No default template found for category: " + category + " and database type: " + dbType,
                    ConfigurationException.ERROR_CODE_MISSING_CONFIG
                );
            }
            return template;
        } catch (Exception e) {
            throw new ConfigurationException(
                "Failed to get template for category: " + category + ", name: " + templateName + ", database type: " + dbType,
                e,
                ConfigurationException.ERROR_CODE_INVALID_CONFIG
            );
        }
    }

    public void setRuntimeProperty(String key, String value) {
        runtimeProperties.put(key, value);
    }

    public String getRuntimeProperty(String key) {
        return runtimeProperties.get(key);
    }

    public Map<String, String> getRuntimeProperties() {
        return new ConcurrentHashMap<>(runtimeProperties);
    }

    public boolean isValidDbType(String dbType) {
        return dbType != null && databaseTypes.containsKey(dbType.toLowerCase());
    }

    public int getDefaultPort(String dbType) {
        DatabaseTypeConfig config = getDatabaseConfig(dbType);
        return config.getDefaultPort();
    }

    public Map<String, String> getDatabaseProperties(String dbType) {
        DatabaseTypeConfig config = getDatabaseConfig(dbType);
        return config.getProperties();
    }
} 