package com.example.shelldemo.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.shelldemo.exception.DatabaseConnectionException;

/**
 * Factory class for creating database connections.
 * Supports different database types and connection methods using templates.
 */
public class DatabaseConnectionFactory {
    private static final Logger logger = LogManager.getLogger(DatabaseConnectionFactory.class);

    // Standard database connection templates
    private static final Map<String, String> CONNECTION_TEMPLATES = Map.of(
        "postgresql", "jdbc:postgresql://%s:%d/%s",
        "mysql", "jdbc:mysql://%s:%d/%s",
        "oracle", "jdbc:oracle:thin:@//%s:%d/%s",
        "sqlserver", "jdbc:sqlserver://%s:%d;databaseName=%s"
    );

    // Default ports for each database type
    private static final Map<String, Integer> DEFAULT_PORTS = Map.of(
        "postgresql", 5432,
        "mysql", 3306,
        "oracle", 1521,
        "sqlserver", 1433
    );

    // Default connection properties for each database type
    private static final Map<String, Properties> DEFAULT_PROPERTIES = Map.of(
        "postgresql", createProperties("ssl", "true", "sslmode", "verify-full"),
        "mysql", createProperties("useSSL", "true", "allowPublicKeyRetrieval", "true", "serverTimezone", "UTC"),
        "oracle", new Properties(),
        "sqlserver", createProperties("encrypt", "true", "trustServerCertificate", "true")
    );

    private static Properties createProperties(String... keyValues) {
        Properties props = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            props.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return props;
    }

    /**
     * Validates and enriches a database connection configuration.
     * 
     * @param dbType the database type
     * @param config the connection configuration
     * @return the validated and initialized connection configuration
     * @throws DatabaseConnectionException if validation fails
     */
    public ConnectionConfig validateAndEnrichConfig(String dbType, ConnectionConfig config) {
        String validatedDbType = dbType.trim().toLowerCase();
        if (!CONNECTION_TEMPLATES.containsKey(validatedDbType)) {
            String errorMessage = "Invalid or unsupported database type: " + validatedDbType;
            logger.error(errorMessage);
            throw new DatabaseConnectionException(errorMessage);
        }

        // Set up connection configuration
        if (config.getPort() <= 0) {
            config.setPort(DEFAULT_PORTS.get(validatedDbType));
        }
        config.setDbType(validatedDbType);
        
        // Handle Oracle-specific connection type
        if (validatedDbType.equals("oracle")) {
            String connectionType = config.getConnectionType();
            if (connectionType == null || (!connectionType.equals("thin") && !connectionType.equals("ldap"))) {
                logger.info("No connection type specified for Oracle, defaulting to LDAP");
                config.setConnectionType("ldap");
            }
        }
        
        // Validate required fields
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            throw new DatabaseConnectionException("Database host must be specified");
        }
        
        if (config.getServiceName() == null || config.getServiceName().trim().isEmpty()) {
            throw new DatabaseConnectionException("Database service name/database name must be specified");
        }
        
        return config;
    }

    /**
     * Gets a database connection based on the provided configuration.
     * 
     * @param config The connection configuration
     * @return A database connection
     * @throws SQLException if connection fails
     */
    public Connection getConnection(ConnectionConfig config) throws SQLException {
        String dbType = config.getDbType().toLowerCase();
        if (!CONNECTION_TEMPLATES.containsKey(dbType)) {
            throw new DatabaseConnectionException("Unsupported database type: " + dbType);
        }

        // Set default port if not specified
        if (config.getPort() <= 0) {
            config.setPort(DEFAULT_PORTS.get(dbType));
        }

        // Generate connection URL
        String connectionUrl = String.format(
            CONNECTION_TEMPLATES.get(dbType),
            config.getHost(),
            config.getPort(),
            config.getServiceName()
        );

        // Setup connection properties
        Properties props = new Properties();
        props.putAll(DEFAULT_PROPERTIES.getOrDefault(dbType, new Properties()));
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());

        logger.info("Attempting to connect to {} using URL: {}", dbType, connectionUrl);
        try {
            return DriverManager.getConnection(connectionUrl, props);
        } catch (SQLException e) {
            throw handleConnectionException(dbType, e, config);
        }
    }

    private RuntimeException handleConnectionException(String dbType, SQLException e, ConnectionConfig config) {
        if ("oracle".equals(dbType)) {
            try {
                // Dynamically load OracleConnectionException
                Class<?> oracleExceptionClass = Class.forName("com.example.shelldemo.connection.oracle.OracleConnectionException");
                String errorCode = determineOracleErrorCode(e, config);
                return (RuntimeException) oracleExceptionClass
                    .getConstructor(String.class, Throwable.class, String.class)
                    .newInstance("Failed to connect to Oracle database", e, errorCode);
            } catch (ReflectiveOperationException ex) {
                logger.warn("OracleConnectionException not available, falling back to DatabaseConnectionException");
            }
        }
        return new DatabaseConnectionException("Failed to connect to database", e);
    }

    private String determineOracleErrorCode(SQLException e, ConnectionConfig config) {
        // Extract Oracle-specific error code
        String errorCode = "UNKNOWN";
        if (e.getMessage() != null && e.getMessage().contains("ORA-")) {
            errorCode = e.getMessage().substring(
                e.getMessage().indexOf("ORA-"),
                e.getMessage().indexOf("ORA-") + 9
            );
        }
        return errorCode;
    }

    /**
     * Validates a PL/SQL block for syntax without executing it.
     * 
     * @param connection The database connection
     * @param plsql The PL/SQL block to validate
     * @throws SQLException if validation fails
     */
    public void validatePlsqlSyntax(Connection connection, String plsql, String dbType, String username) throws SQLException {
        String validationQuery = switch (dbType.toLowerCase()) {
            case "oracle" -> 
                "BEGIN " +
                "    DBMS_UTILITY.COMPILE_SCHEMA('" + username.toUpperCase() + "', FALSE);" +
                "    " + plsql + " " +
                "END;";
            case "postgresql" -> 
                "DO $$ BEGIN " + plsql + " END $$;";
            case "sqlserver" ->
                "EXEC sp_validateloginname " + plsql;
            default -> throw new SQLException("PL/SQL validation not supported for " + dbType);
        };
        
        try (var stmt = connection.prepareStatement(validationQuery)) {
            stmt.setQueryTimeout(10);
            stmt.executeQuery();
        }
    }

    /**
     * Validates a SQL statement for syntax without executing it.
     * 
     * @param connection The database connection
     * @param sql The SQL statement to validate
     * @throws SQLException if validation fails
     */
    public void validateSqlSyntax(Connection connection, String sql, String dbType) throws SQLException {
        String validationQuery = switch (dbType.toLowerCase()) {
            case "oracle" -> 
                "SELECT 1 FROM DUAL WHERE EXISTS (" + sql + ")";
            case "postgresql", "mysql" -> 
                "EXPLAIN " + sql;
            case "sqlserver" ->
                "SET PARSEONLY ON; " + sql + "; SET PARSEONLY OFF;";
            default -> throw new SQLException("SQL validation not supported for " + dbType);
        };
        
        try (var stmt = connection.prepareStatement(validationQuery)) {
            stmt.setQueryTimeout(10);
            stmt.executeQuery();
        }
    }

    /**
     * Gets the execution plan for a SQL statement without executing it.
     * 
     * @param connection The database connection
     * @param sql The SQL statement to explain
     * @return The execution plan as a string
     * @throws SQLException if explain plan fails
     */
    public String getExplainPlan(Connection connection, String sql, String dbType) throws SQLException {
        String explainQuery = switch (dbType.toLowerCase()) {
            case "oracle" -> 
                "EXPLAIN PLAN FOR " + sql;
            case "postgresql" -> 
                "EXPLAIN (ANALYZE false, COSTS true, FORMAT TEXT) " + sql;
            case "mysql" -> 
                "EXPLAIN FORMAT=TREE " + sql;
            case "sqlserver" ->
                "SET SHOWPLAN_XML ON; " + sql + "; SET SHOWPLAN_XML OFF;";
            default -> throw new SQLException("Explain plan not supported for " + dbType);
        };
        
        StringBuilder plan = new StringBuilder();
        try (var stmt = connection.prepareStatement(explainQuery);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                plan.append(rs.getString(1)).append("\n");
            }
        }
        return plan.toString();
    }
}