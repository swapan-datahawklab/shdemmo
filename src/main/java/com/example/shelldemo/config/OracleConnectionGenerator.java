package com.example.shelldemo.config;

import com.example.shelldemo.model.domain.ConnectionConfig;
import com.example.shelldemo.model.domain.LdapServerConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class OracleConnectionGenerator {
    private static final Logger logger = LogManager.getLogger(OracleConnectionGenerator.class);
    private static final String DEFAULT_CONFIG_PATH = "application.yaml";
    private static final String ORACLE_CONFIG_KEY = "oracle";
    private final YamlConfigReader configReader;
    private final TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
    private final TypeReference<Map<String, Object>> oracleConfigType = new TypeReference<>() {};
    private final TypeReference<Map<String, Object>> ldapConfigType = new TypeReference<>() {};

    public OracleConnectionGenerator() throws IOException {
        this.configReader = new YamlConfigReader(DEFAULT_CONFIG_PATH);
    }

    // Oracle Thin Client methods
    public String generateThinConnectionString() {
        return generateThinConnectionString(DEFAULT_CONFIG_PATH);
    }

    public String generateThinConnectionString(String configPath) {
        logger.debug("Generating Oracle Thin connection string from config: {}", configPath);
        Map<String, Object> config = configReader.readConfig(configPath, mapType);
        Map<String, Object> oracleConfig = configReader.convertValue(config.get(ORACLE_CONFIG_KEY), oracleConfigType);
        
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost((String) oracleConfig.get("host"));
        connectionConfig.setPort((Integer) oracleConfig.get("port"));
        connectionConfig.setServiceName((String) oracleConfig.get("serviceName"));
        return connectionConfig.getConnectionUrl();
    }

    // LDAP methods
    public String generateLdapConnectionString() {
        return generateLdapConnectionString(DEFAULT_CONFIG_PATH);
    }

    public String generateLdapConnectionString(String configPath) {
        logger.debug("Generating Oracle LDAP connection string from config: {}", configPath);
        Map<String, Object> config = configReader.readConfig(configPath, mapType);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
        LdapServerConfig ldapServerConfig = new LdapServerConfig();
        ldapServerConfig.setHost((String) ldapConfig.get("host"));
        ldapServerConfig.setPort((Integer) ldapConfig.get("port"));
        ldapServerConfig.setBaseDn((String) ldapConfig.get("baseDn"));
        ldapServerConfig.setUseSsl((Boolean) ldapConfig.get("ssl"));
        return generateLdapConnectionString(ldapServerConfig);
    }

    public String generateLdapConnectionString(LdapServerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("LDAP connection config cannot be null");
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:oracle:thin:@ldap://");
        jdbcUrl.append(config.getHost())
               .append(":")
               .append(config.getPort())
               .append("/")
               .append(config.getBaseDn())
               .append(",cn=OracleContext,dc=oracle,dc=com");

        logger.debug("Generated Oracle LDAP connection string: {}", jdbcUrl);
        return jdbcUrl.toString();
    }

    public Properties generateLdapProperties() {
        return generateLdapProperties(DEFAULT_CONFIG_PATH);
    }

    public Properties generateLdapProperties(String configPath) {
        logger.debug("Generating LDAP properties from config: {}", configPath);
        Map<String, Object> config = configReader.readConfig(configPath, mapType);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
        LdapServerConfig ldapServerConfig = new LdapServerConfig();
        ldapServerConfig.setHost((String) ldapConfig.get("host"));
        ldapServerConfig.setPort((Integer) ldapConfig.get("port"));
        ldapServerConfig.setBaseDn((String) ldapConfig.get("baseDn"));
        ldapServerConfig.setBindDn((String) ldapConfig.get("bindDn"));
        ldapServerConfig.setBindPassword((String) ldapConfig.get("bindPassword"));
        ldapServerConfig.setUseSsl((Boolean) ldapConfig.get("ssl"));
        ldapServerConfig.setConnectionTimeout((Integer) ldapConfig.getOrDefault("connectionTimeout", 5000));
        ldapServerConfig.setReadTimeout((Integer) ldapConfig.getOrDefault("readTimeout", 5000));
        return generateLdapProperties(ldapServerConfig);
    }

    public Properties generateLdapProperties(LdapServerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("LDAP connection config cannot be null");
        }

        Properties props = new Properties();
        props.setProperty("java.naming.provider.url", generateLdapUrl(config));
        props.setProperty("java.naming.security.authentication", "simple");
        props.setProperty("java.naming.security.principal", config.getBindDn());
        props.setProperty("java.naming.security.credentials", config.getBindPassword());
        props.setProperty("com.sun.jndi.ldap.connect.timeout", String.valueOf(config.getConnectionTimeout()));
        props.setProperty("com.sun.jndi.ldap.read.timeout", String.valueOf(config.getReadTimeout()));

        logger.debug("Generated LDAP properties");
        return props;
    }

    private String generateLdapUrl(LdapServerConfig config) {
        StringBuilder url = new StringBuilder("ldap");
        if (config.isUseSsl()) {
            url.append("s");
        }
        url.append("://")
           .append(config.getHost())
           .append(":")
           .append(config.getPort())
           .append("/")
           .append(config.getBaseDn());

        logger.debug("Generated LDAP URL: {}", url);
        return url.toString();
    }

    // Common methods
    public String getUsername() {
        return getUsername(DEFAULT_CONFIG_PATH);
    }

    public String getUsername(String configPath) {
        Map<String, Object> config = configReader.readConfig(configPath, mapType);
        Map<String, Object> oracleConfig = configReader.convertValue(config.get(ORACLE_CONFIG_KEY), oracleConfigType);
        return (String) oracleConfig.get("username");
    }

    public String getPassword() {
        return getPassword(DEFAULT_CONFIG_PATH);
    }

    public String getPassword(String configPath) {
        Map<String, Object> config = configReader.readConfig(configPath, mapType);
        Map<String, Object> oracleConfig = configReader.convertValue(config.get(ORACLE_CONFIG_KEY), oracleConfigType);
        return (String) oracleConfig.get("password");
    }

    public String getBindDn() {
        return getBindDn(DEFAULT_CONFIG_PATH);
    }

    public String getBindDn(String configPath) {
        Map<String, Object> config = configReader.readConfig(configPath, mapType);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
        return (String) ldapConfig.get("bindDn");
    }

    public String getBindPassword() {
        return getBindPassword(DEFAULT_CONFIG_PATH);
    }

    public String getBindPassword(String configPath) {
        Map<String, Object> config = configReader.readConfig(configPath, mapType);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), ldapConfigType);
        return (String) ldapConfig.get("bindPassword");
    }
} 