package com.example.shelldemo.connection;

import com.example.shelldemo.config.YamlConfigReader;
import com.example.shelldemo.model.entity.config.LdapServerConfig;
import com.example.shelldemo.exception.ConfigurationException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Manages Oracle LDAP connections and configuration.
 * Provides dedicated Oracle LDAP support for database connections.
 */
public class OracleLdapConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(OracleLdapConnectionManager.class);
    private static final String DEFAULT_CONFIG_PATH = "application.yaml";
    private final YamlConfigReader configReader;
    private final TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
    private final TypeReference<Map<String, Object>> ldapConfigType = new TypeReference<>() {};

    public OracleLdapConnectionManager() throws IOException {
        logger.debug("Initializing OracleLdapConnectionManager with default config path: {}", DEFAULT_CONFIG_PATH);
        this.configReader = new YamlConfigReader(DEFAULT_CONFIG_PATH);
        logger.info("OracleLdapConnectionManager initialized successfully");
    }

    public String generateConnectionString() throws IOException {
        logger.debug("Generating connection string using default config path");
        return generateConnectionString(DEFAULT_CONFIG_PATH);
    }

    public String generateConnectionString(String configPath) throws IOException {
        logger.debug("Generating Oracle LDAP connection string from config: {}", configPath);
        try {
            Map<String, Object> config = configReader.readConfig(configPath, mapType);
            if (config == null || !config.containsKey("ldap")) {
                String errorMessage = "Missing LDAP configuration in " + configPath;
                logger.error(errorMessage);
                throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
            }

            Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
            LdapServerConfig ldapServerConfig = createLdapConfig(ldapConfig);
            String connectionString = generateConnectionString(ldapServerConfig);
            logger.info("Successfully generated LDAP connection string");
            return connectionString;
        } catch (IOException e) {
            logger.error("Failed to read LDAP configuration from {}: {}", configPath, e.getMessage());
            throw new ConfigurationException("Failed to read LDAP configuration", e, 
                ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    public String generateConnectionString(LdapServerConfig config) {
        logger.debug("Generating connection string from LDAP server config");
        if (config == null) {
            String errorMessage = "LDAP connection config cannot be null";
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:oracle:thin:@ldap://");
        jdbcUrl.append(config.getHost())
               .append(":")
               .append(config.getPort())
               .append("/")
               .append(config.getBaseDn())
               .append(",cn=OracleContext,dc=oracle,dc=com");

        String url = jdbcUrl.toString();
        logger.debug("Generated Oracle LDAP connection string: {}", url);
        return url;
    }

    public Properties generateProperties() throws IOException {
        logger.debug("Generating properties using default config path");
        return generateProperties(DEFAULT_CONFIG_PATH);
    }

    public Properties generateProperties(String configPath) throws IOException {
        logger.debug("Generating Oracle LDAP properties from config: {}", configPath);
        try {
            Map<String, Object> config = configReader.readConfig(configPath, mapType);
            if (config == null || !config.containsKey("ldap")) {
                String errorMessage = "Missing LDAP configuration in " + configPath;
                logger.error(errorMessage);
                throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
            }

            Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
            LdapServerConfig ldapServerConfig = createLdapConfig(ldapConfig);
            Properties props = generateProperties(ldapServerConfig);
            logger.info("Successfully generated LDAP properties");
            return props;
        } catch (IOException e) {
            logger.error("Failed to read LDAP configuration from {}: {}", configPath, e.getMessage());
            throw new ConfigurationException("Failed to read LDAP configuration", e, 
                ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    public Properties generateProperties(LdapServerConfig config) {
        logger.debug("Generating properties from LDAP server config");
        if (config == null) {
            String errorMessage = "LDAP connection config cannot be null";
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_INVALID_CONFIG);
        }

        Properties props = new Properties();
        props.setProperty("java.naming.provider.url", generateLdapUrl(config));
        props.setProperty("java.naming.security.authentication", "simple");
        props.setProperty("java.naming.security.principal", config.getBindDn());
        props.setProperty("java.naming.security.credentials", "********"); // Masked for logging
        props.setProperty("com.sun.jndi.ldap.connect.timeout", String.valueOf(config.getConnectionTimeout()));
        props.setProperty("com.sun.jndi.ldap.read.timeout", String.valueOf(config.getReadTimeout()));

        logger.debug("Generated Oracle LDAP properties: {}", maskSensitiveProperties(props));
        return props;
    }

    private String generateLdapUrl(LdapServerConfig config) {
        logger.debug("Generating LDAP URL for host: {}", config.getHost());
        StringBuilder url = new StringBuilder("ldap");
        if (config.isUseSsl()) {
            url.append("s");
            logger.debug("Using SSL for LDAP connection");
        }
        url.append("://")
           .append(config.getHost())
           .append(":")
           .append(config.getPort())
           .append("/")
           .append(config.getBaseDn());

        String ldapUrl = url.toString();
        logger.debug("Generated Oracle LDAP URL: {}", ldapUrl);
        return ldapUrl;
    }

    private LdapServerConfig createLdapConfig(Map<String, Object> ldapConfig) {
        logger.debug("Creating LDAP server config from map");
        try {
            LdapServerConfig config = new LdapServerConfig();
            config.setHost((String) ldapConfig.get("host"));
            config.setPort((Integer) ldapConfig.get("port"));
            config.setBaseDn((String) ldapConfig.get("baseDn"));
            config.setBindDn((String) ldapConfig.get("bindDn"));
            config.setBindPassword((String) ldapConfig.get("bindPassword"));
            config.setUseSsl((Boolean) ldapConfig.get("ssl"));
            config.setConnectionTimeout((Integer) ldapConfig.getOrDefault("connectionTimeout", 5000));
            config.setReadTimeout((Integer) ldapConfig.getOrDefault("readTimeout", 5000));
            logger.debug("Created LDAP server config for host: {}", config.getHost());
            return config;
        } catch (Exception e) {
            String errorMessage = "Failed to create LDAP server config: " + e.getMessage();
            logger.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e, ConfigurationException.ERROR_CODE_PARSE_ERROR);
        }
    }

    private Properties maskSensitiveProperties(Properties props) {
        Properties masked = new Properties();
        props.forEach((key, value) -> {
            if (key.toString().toLowerCase().contains("password") || 
                key.toString().toLowerCase().contains("credentials")) {
                masked.setProperty(key.toString(), "********");
            } else {
                masked.setProperty(key.toString(), value.toString());
            }
        });
        return masked;
    }

    public String getBindDn() throws IOException {
        logger.debug("Getting bind DN using default config path");
        return getBindDn(DEFAULT_CONFIG_PATH);
    }

    public String getBindDn(String configPath) throws IOException {
        logger.debug("Getting bind DN from config: {}", configPath);
        try {
            Map<String, Object> config = configReader.readConfig(configPath, mapType);
            Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
            String bindDn = (String) ldapConfig.get("bindDn");
            if (bindDn == null) {
                String errorMessage = "Missing bindDn in LDAP configuration";
                logger.error(errorMessage);
                throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
            }
            return bindDn;
        } catch (IOException e) {
            logger.error("Failed to read bind DN from config: {}", e.getMessage());
            throw new ConfigurationException("Failed to read bind DN", e, 
                ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }

    public String getBindPassword() throws IOException {
        logger.debug("Getting bind password using default config path");
        return getBindPassword(DEFAULT_CONFIG_PATH);
    }

    public String getBindPassword(String configPath) throws IOException {
        logger.debug("Getting bind password from config: {}", configPath);
        try {
            Map<String, Object> config = configReader.readConfig(configPath, mapType);
            Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
            String bindPassword = (String) ldapConfig.get("bindPassword");
            if (bindPassword == null) {
                String errorMessage = "Missing bindPassword in LDAP configuration";
                logger.error(errorMessage);
                throw new ConfigurationException(errorMessage, ConfigurationException.ERROR_CODE_MISSING_CONFIG);
            }
            return bindPassword;
        } catch (IOException e) {
            logger.error("Failed to read bind password from config: {}", e.getMessage());
            throw new ConfigurationException("Failed to read bind password", e, 
                ConfigurationException.ERROR_CODE_FILE_NOT_FOUND);
        }
    }
} 