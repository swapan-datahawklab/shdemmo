package com.example.shelldemo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;

/**
 * Implementation of ConfigReader for YAML configuration files.
 * Loads and caches configuration at startup for better performance.
 */
class YamlConfigReader extends AbstractConfigReader {
    private static final Logger logger = LoggerFactory.getLogger(YamlConfigReader.class);
    private final Map<String, Object> cachedConfig;
    private final String configFilePath;

    /**
     * Creates a new YamlConfigReader and loads the configuration from the specified file.
     *
     * @param configFilePath Path to the YAML configuration file
     * @throws IOException If there's an error reading the configuration file
     */
    public YamlConfigReader(String configFilePath) throws IOException {
        super(new ObjectMapper(new YAMLFactory()));
        logger.debug("Initializing YamlConfigReader with config path: {}", configFilePath);
        this.configFilePath = configFilePath;
        this.cachedConfig = loadConfig();
        logger.info("YamlConfigReader initialized successfully with {} configuration entries", cachedConfig.size());
    }

    /**
     * Loads the configuration from the YAML file.
     *
     * @return The loaded configuration as a Map
     * @throws IOException If there's an error reading the file
     */
    private Map<String, Object> loadConfig() throws IOException {
        logger.debug("Loading configuration from file: {}", configFilePath);
        try {
            // First try loading from classpath
            var inputStream = YamlConfigReader.class.getClassLoader().getResourceAsStream(configFilePath);
            if (inputStream == null) {
                inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFilePath);
            }
            if (inputStream == null) {
                // If not found in classpath, try as a file
                File file = validateAndGetFile(configFilePath);
                inputStream = file.toURI().toURL().openStream();
            }
            
            Map<String, Object> config = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
            logger.debug("Successfully loaded {} configuration entries", config.size());
            
            // Log each top-level configuration section with details
            logger.debug("Configuration sections loaded:");
            config.forEach((key, value) -> {
                if (value instanceof Map<?,?> section) {
                    logger.debug("Section '{}' contains {} entries:", key, section.size());
                    
                    if (key.equals("spring")) {
                        section.forEach((k, v) -> logger.debug("  spring.{} = {}", k, v));
                    }
                    else if (key.equals("databases")) {
                        // Print database types
                        if (section.containsKey("types")) {
                            if (section.get("types") instanceof Map<?,?> types) {
                                logger.debug("  Database types:");
                                types.forEach((dbType, typeConfig) -> {
                                    logger.debug("    {}", dbType);
                                    if (typeConfig instanceof Map<?,?> typeSettings) {
                                        typeSettings.forEach((k, v) -> logger.debug("      {} = {}", k, v));
                                    }
                                });
                            }
                        }
                        // Print database instances
                        if (section.containsKey("instances")) {
                            if (section.get("instances") instanceof List<?> instances) {
                                logger.debug("  Database instances:");
                                instances.forEach(instance -> {
                                    if (instance instanceof Map<?,?> dbInstance) {
                                        logger.debug("    {} -> {}", dbInstance.get("name"), dbInstance.get("serviceName"));
                                    }
                                });
                            }
                        }
                    }
                    else if (key.equals("ldap")) {
                        section.forEach((k, v) -> {
                            if (k.equals("servers")) {
                                logger.debug("  LDAP servers:");
                                if (v instanceof List<?> servers) {
                                    servers.forEach(server -> {
                                        if (server instanceof Map<?,?> serverConfig) {
                                            logger.debug("    {} (port: {}, ssl: {})", 
                                                serverConfig.get("host"), serverConfig.get("port"), serverConfig.get("ssl"));
                                        }
                                    });
                                }
                            } else {
                                logger.debug("  ldap.{} = {}", k, v);
                            }
                        });
                    }
                    else if (key.equals("database")) {
                        section.forEach((dbType, dbConfig) -> {
                            logger.debug("  Database '{}' configuration:", dbType);
                            if (dbConfig instanceof Map<?,?> dbSettings) {
                                dbSettings.forEach((k, v) -> {
                                    if (v instanceof Map<?,?> subConfig) {
                                        logger.debug("    {}:", k);
                                        subConfig.forEach((subK, subV) -> 
                                            logger.debug("      {} = {}", subK, 
                                                (subK instanceof String && ((String)subK).contains("password")) ? "********" : subV));
                                    } else {
                                        logger.debug("    {} = {}", k, v);
                                    }
                                });
                            }
                        });
                    }
                    else if (key.equals("logging")) {
                        section.forEach((k, v) -> {
                            if (v instanceof Map<?,?> logConfig) {
                                logger.debug("  logging.{}:", k);
                                logConfig.forEach((subK, subV) -> 
                                    logger.debug("    {} = {}", subK, subV));
                            } else {
                                logger.debug("  logging.{} = {}", k, v);
                            }
                        });
                    }
                }
            });
            
            return config;
        } catch (IOException e) {
            String errorMessage = "Failed to load configuration from " + configFilePath;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    /**
     * Gets a specific configuration section by key.
     *
     * @param key The configuration key to retrieve
     * @return The configuration value for the specified key
     */
    public Object getConfigSection(String key) {
        logger.trace("Retrieving configuration section for key: {}", key);
        Object value = cachedConfig.get(key);
        if (value == null) {
            logger.debug("No configuration found for key: {}", key);
        }
        return value;
    }

    /**
     * Gets a specific configuration section by key and converts it to the specified type.
     *
     * @param key The configuration key to retrieve
     * @param valueType The type to convert the configuration to
     * @return The configuration value converted to the specified type
     */
    public <T> T getConfigSection(String key, Class<T> valueType) {
        logger.trace("Retrieving and converting configuration section for key: {} to type: {}", 
            key, valueType.getSimpleName());
        Object value = cachedConfig.get(key);
        if (value == null) {
            logger.debug("No configuration found for key: {}", key);
            return null;
        }
        try {
            T converted = objectMapper.convertValue(value, valueType);
            logger.trace("Successfully converted configuration value to {}", valueType.getSimpleName());
            return converted;
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format("Failed to convert configuration value for key '%s' to type %s", 
                key, valueType.getSimpleName());
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T readConfig(String path, TypeReference<T> typeRef) throws IOException {
        logger.debug("Reading configuration for path: {}", path);
        try {
            // Split the path into parts
            String[] pathParts = path.split("\\.");
            Object currentValue = cachedConfig;
            
            // Navigate through the path
            for (String part : pathParts) {
                if (currentValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) currentValue;
                    currentValue = map.get(part);
                    if (currentValue == null) {
                        String errorMessage = "Configuration not found for path: " + path;
                        logger.error(errorMessage);
                        throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
                    }
                } else {
                    String errorMessage = "Invalid path: " + path;
                    logger.error(errorMessage);
                    throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
                }
            }
            
            T config = objectMapper.convertValue(currentValue, typeRef);
            logger.debug("Successfully read configuration for path: {}", path);
            return config;
        } catch (IllegalArgumentException e) {
            String errorMessage = "Failed to convert configuration for path: " + path;
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    @Override
    public <T> T convertValue(Object value, TypeReference<T> typeRef) {
        logger.trace("Converting configuration value to type: {}", typeRef.getType().getTypeName());
        try {
            T converted = objectMapper.convertValue(value, typeRef);
            logger.trace("Successfully converted configuration value");
            return converted;
        } catch (IllegalArgumentException e) {
            String errorMessage = "Failed to convert configuration value";
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }
} 