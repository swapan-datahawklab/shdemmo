package com.example.shelldemo.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.config.ConfigurationHolder;


/**
 * Factory for creating database connections using validated configurations.
 */
public class DatabaseConnectionFactory {
    private static final Logger logger = LogManager.getLogger(DatabaseConnectionFactory.class);
    private final JdbcDriverLoader driverLoader;

    public DatabaseConnectionFactory() {
        this.driverLoader = new JdbcDriverLoader();
        logger.debug("DatabaseConnectionFactory initialized");
    }

    /**
     * Creates a connection using a pre-configured ConnectionConfig.
     * 
     * @param config The validated connection configuration
     * @return A new database connection
     * @throws SQLException if connection fails
     */
    public Connection createConnection(ConnectionConfig config) throws SQLException {
        logger.info("Creating database connection for type: {}, host: {}", config.getDbType(), config.getHost());
        
        try {
            String url = buildConnectionUrl(config);
            logger.debug("Using connection URL: {}", url);
            
            Properties props = buildConnectionProperties(config);
            if (logger.isDebugEnabled()) {
                logger.debug("Connection properties configured: {}", 
                    props.stringPropertyNames().stream()
                        .filter(key -> !key.contains("password"))
                        .map(key -> key + "=" + props.getProperty(key))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("none"));
            }
            
            Connection conn = DriverManager.getConnection(url, props);
            logger.info("Successfully established connection to {} database at {}:{}",config.getDbType(), config.getHost(), config.getPort());
            return conn;
        } catch (SQLException e) {
            String context = String.format("host=%s, port=%d, service=%s", config.getHost(), config.getPort(), config.getServiceName());
           
            throw DatabaseException.fromSQLException(
                "Failed to establish database connection",
                e,
                config.getDbType(),
                context
            );
        }
    }

    /**
     * Creates a connection using builder pattern.
     * 
     * @param configBuilder A lambda that configures the connection
     * @return A new database connection
     * @throws SQLException if connection fails
     */
    public Connection createConnection(ConnectionConfigBuilder configBuilder) throws SQLException {
        logger.debug("Creating connection using builder pattern");
        ConnectionConfig config = configBuilder.configure(ConnectionConfig.builder()).build();
        return createConnection(config);
    }

    /**
     * Loads a JDBC driver from the specified path before creating a connection.
     * 
     * @param driverPath Path to the JDBC driver
     * @param config The connection configuration
     * @return A new database connection
     * @throws SQLException if connection fails
     */
    public Connection createConnection(String driverPath, ConnectionConfig config) throws SQLException {
        if (driverPath != null && !driverPath.isEmpty()) {
            logger.info("Loading JDBC driver from path: {}", driverPath);
            driverLoader.loadDriver(driverPath);
            logger.debug("JDBC driver loaded successfully");
        }
        return createConnection(config);
    }

    public String buildConnectionUrl(ConnectionConfig config) {
        Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(config.getDbType());
        @SuppressWarnings("unchecked")
        Map<String, Object> templates = (Map<String, Object>) dbmsConfig.get("templates");
        if (templates != null) {
            logger.info("Available templates configuration:");
            templates.forEach((key, value) -> logger.info("  {} -> {}", key, value));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> jdbc = templates != null ? (Map<String, Object>) templates.get("jdbc") : null;
        
        if ("ldap".equalsIgnoreCase(config.getConnectionType())) {
            return buildLdapConnectionUrl(config, templates, jdbc);
        } else {
        String urlTemplate = jdbc != null ? (String) jdbc.get("defaultTemplate") : null;
            if (urlTemplate == null) {
                urlTemplate = jdbc != null ? (String) jdbc.get("default") : null;
            }
        if (urlTemplate == null) {
            throw new DatabaseException(
                String.format("Missing URL template for database type: %s", config.getDbType()),
                ErrorType.CONFIG_NOT_FOUND
            );
        }
        String url = String.format(
            urlTemplate,
            config.getHost(),
            config.getPort(),
            config.getServiceName()
        );
        logger.debug("Built connection URL: {}", url);
            return url;
        }
    }

    private String buildLdapConnectionUrl(ConnectionConfig config, Map<String, Object> templates, Map<String, Object> jdbc) {
        String urlTemplate = jdbc != null ? (String) jdbc.get("ldap") : null;
        if (urlTemplate == null) {
            throw new DatabaseException("Missing LDAP URL template for Oracle", ErrorType.CONFIG_NOT_FOUND);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> ldapConfig = (Map<String, Object>) templates.get("ldap");
        if (ldapConfig == null) {
            throw new DatabaseException("Missing LDAP config for Oracle", ErrorType.CONFIG_NOT_FOUND);
        }
        @SuppressWarnings("unchecked")
        java.util.List<String> servers = (java.util.List<String>) ldapConfig.get("servers");
        String context = (String) ldapConfig.get("context");
        Object portObj = ldapConfig.get("port");
        int port = (portObj instanceof Number number) ? number.intValue() : 389;
        String service = config.getServiceName();

        if (servers == null || servers.isEmpty() || context == null || service == null) {
            throw new DatabaseException("Incomplete LDAP configuration for Oracle", ErrorType.CONFIG_INVALID);
        }

        StringBuilder hosts = new StringBuilder();
        for (int i = 0; i < servers.size(); i++) {
            if (i > 0) hosts.append(" ");
            hosts.append(String.format("ldap://%s:%d/%s,%s", servers.get(i), port, service, context));
        }
        String url = String.format(urlTemplate, hosts.toString());
        logger.debug("Built LDAP connection URL: {}", url);
        return url;
    }

    private Properties buildConnectionProperties(ConnectionConfig config) {
        Properties props = new Properties();
        Map<String, Object> dbmsConfig = ConfigurationHolder.getInstance().getDatabaseConfig(config.getDbType());
        
        // Make connection-properties optional
        try {
            Map<String, Object> connProps = ConnectionConfig.getConfigMap(dbmsConfig, "properties");
            if (connProps != null) {
                logger.debug("Applying {} database-specific connection properties", connProps.size());
                for (Map.Entry<String, Object> entry : connProps.entrySet()) {
                    props.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        } catch (DatabaseException e) {
            // Log as debug since this is optional
            logger.debug("No additional connection properties found for database type: {}", config.getDbType());
        }
        
        // Always set these required properties
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());
        
        logger.debug("Connection properties set for user: {}", config.getUsername());
        return props;
    }

    @FunctionalInterface
    public interface ConnectionConfigBuilder {
        ConnectionConfig.Builder configure(ConnectionConfig.Builder builder);
    }
}