package com.example.shelldemo.config;

import com.example.shelldemo.model.ConnectionConfig;
import com.example.shelldemo.model.LdapServerConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class OracleConnectionGenerator {
    private static final Logger logger = LogManager.getLogger(OracleConnectionGenerator.class);
    private static final String DEFAULT_CONFIG_PATH = "application.yaml";
    private final YamlConfigReader configReader;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> ORACLE_CONFIG_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> LDAP_CONFIG_TYPE = new TypeReference<>() {};

    public OracleConnectionGenerator() {
        this.configReader = new YamlConfigReader();
    }

    // Oracle Thin Client methods
    public String generateThinConnectionString() throws IOException {
        return generateThinConnectionString(DEFAULT_CONFIG_PATH);
    }

    public String generateThinConnectionString(String configPath) throws IOException {
        logger.debug("Generating Oracle Thin connection string from config: {}", configPath);
        Map<String, Object> config = configReader.readConfig(configPath, MAP_TYPE);
        Map<String, Object> oracleConfig = configReader.convertValue(config.get("oracle"), ORACLE_CONFIG_TYPE);
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setHost((String) oracleConfig.get("host"));
        connectionConfig.setPort((Integer) oracleConfig.get("port"));
        connectionConfig.setServiceName((String) oracleConfig.get("serviceName"));
        return connectionConfig.getConnectionUrl();
    }

    // LDAP methods
    public String generateLdapConnectionString() throws IOException {
        return generateLdapConnectionString(DEFAULT_CONFIG_PATH);
    }

    public String generateLdapConnectionString(String configPath) throws IOException {
        logger.debug("Generating Oracle LDAP connection string from config: {}", configPath);
        Map<String, Object> config = configReader.readConfig(configPath, MAP_TYPE);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), LDAP_CONFIG_TYPE);
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

    public Properties generateLdapProperties() throws IOException {
        return generateLdapProperties(DEFAULT_CONFIG_PATH);
    }

    public Properties generateLdapProperties(String configPath) throws IOException {
        logger.debug("Generating LDAP properties from config: {}", configPath);
        Map<String, Object> config = configReader.readConfig(configPath, MAP_TYPE);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), LDAP_CONFIG_TYPE);
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
    public String getUsername() throws IOException {
        return getUsername(DEFAULT_CONFIG_PATH);
    }

    public String getUsername(String configPath) throws IOException {
        Map<String, Object> config = configReader.readConfig(configPath, MAP_TYPE);
        Map<String, Object> oracleConfig = configReader.convertValue(config.get("oracle"), ORACLE_CONFIG_TYPE);
        return (String) oracleConfig.get("username");
    }

    public String getPassword() throws IOException {
        return getPassword(DEFAULT_CONFIG_PATH);
    }

    public String getPassword(String configPath) throws IOException {
        Map<String, Object> config = configReader.readConfig(configPath, MAP_TYPE);
        Map<String, Object> oracleConfig = configReader.convertValue(config.get("oracle"), ORACLE_CONFIG_TYPE);
        return (String) oracleConfig.get("password");
    }

    public String getBindDn() throws IOException {
        return getBindDn(DEFAULT_CONFIG_PATH);
    }

    public String getBindDn(String configPath) throws IOException {
        Map<String, Object> config = configReader.readConfig(configPath, MAP_TYPE);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), LDAP_CONFIG_TYPE);
        return (String) ldapConfig.get("bindDn");
    }

    public String getBindPassword() throws IOException {
        return getBindPassword(DEFAULT_CONFIG_PATH);
    }

    public String getBindPassword(String configPath) throws IOException {
        Map<String, Object> config = configReader.readConfig(configPath, MAP_TYPE);
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), LDAP_CONFIG_TYPE);
        return (String) ldapConfig.get("bindPassword");
    }
} 