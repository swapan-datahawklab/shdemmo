package com.example.shelldemo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.shelldemo.model.entity.config.ConnectionConfig;
import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.exception.DatabaseConnectionException;
import com.example.shelldemo.exception.DatabaseOperationException;
import com.example.shelldemo.connection.DatabaseConnectionFactory;

/**
 * Unified database operations class.
 * Provides common functionality for all database types using JDBC.
 */
public class UnifiedDatabaseOperation implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedDatabaseOperation.class);
    
    private static final Map<String, String> TEST_QUERIES = Map.of(
        "oracle", "SELECT 1 FROM DUAL",
        "postgresql", "SELECT 1",
        "mysql", "SELECT 1",
        "sqlserver", "SELECT 1"
    );

    private static final Map<String, String> PROC_CALL_TEMPLATES = Map.of(
        "oracle", "{call %s(%s)}",
        "postgresql", "CALL %s(%s)",
        "mysql", "{call %s(%s)}",
        "sqlserver", "{call %s(%s)}"
    );

    private final String dbType;
    private final ConnectionConfig config;
    private final Connection connection;
    private final DatabaseConnectionFactory connectionFactory;

    public UnifiedDatabaseOperation(String dbType, ConnectionConfig config, DatabaseConnectionFactory connectionFactory) {
        logger.debug("Creating UnifiedDatabaseOperation for type: {}, host: {}", dbType, config.getHost());
        this.dbType = dbType.toLowerCase();
        this.config = config;
        this.connectionFactory = connectionFactory;
        this.connection = createConnection();
        logger.info("Database operation initialized successfully for {}", dbType);
    }

    private Connection createConnection() {
        try {
            logger.debug("Attempting to create database connection to {}:{}", config.getHost(), config.getPort());
            Connection conn = connectionFactory.getConnection(config);
            logger.info("Successfully established database connection");
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to create database connection: {}", e.getMessage());
            throw new DatabaseConnectionException("Failed to create database connection", e);
        }
    }

    public static UnifiedDatabaseOperation create(String dbType, ConnectionConfig config, DatabaseConnectionFactory connectionFactory) {
        return new UnifiedDatabaseOperation(dbType, config, connectionFactory);
    }

    public String getDatabaseType() {
        return dbType;
    }

    public ConnectionConfig getConnectionConfig() {
        return config;
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection conn) throws SQLException;
    }

    public <T> T execute(SqlFunction<T> operation) {
        try {
            return operation.apply(connection);
        } catch (SQLException e) {
            throw new DatabaseOperationException(formatDatabaseError(e), e);
        }
    }

    private String formatDatabaseError(SQLException e) {
        switch (dbType) {
            case "oracle":
                return formatOracleError(e);
            case "postgresql":
                return formatPostgresError(e);
            case "mysql":
                return formatMySqlError(e);
            case "sqlserver":
                return formatSqlServerError(e);
            default:
                return e.getMessage();
        }
    }

    private String formatOracleError(SQLException e) {
        String message = e.getMessage();
        int oraIndex = message.indexOf("ORA-");
        if (oraIndex >= 0) {
            int endIndex = message.indexOf(":", oraIndex);
            String oraCode = endIndex > oraIndex ? message.substring(oraIndex, endIndex) : message.substring(oraIndex);
            String errorDetails = message.substring(endIndex + 1).trim();
            return String.format("%s: %s", oraCode, errorDetails);
        }
        return message;
    }

    private String formatPostgresError(SQLException e) {
        return String.format("PostgreSQL Error %s: %s", e.getSQLState(), e.getMessage());
    }

    private String formatMySqlError(SQLException e) {
        return String.format("MySQL Error %d: %s", e.getErrorCode(), e.getMessage());
    }

    private String formatSqlServerError(SQLException e) {
        return String.format("SQL Server Error %d: %s", e.getErrorCode(), e.getMessage());
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        logger.debug("Executing query: {}", sql);
        return execute(conn -> {
            List<Map<String, Object>> results = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                    logger.trace("Setting parameter {}: {}", i + 1, params[i]);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    int columnCount = rs.getMetaData().getColumnCount();
                    logger.debug("Query returned {} columns", columnCount);
                    
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = rs.getMetaData().getColumnLabel(i);
                            if (columnName == null || columnName.isEmpty()) {
                                columnName = rs.getMetaData().getColumnName(i);
                            }
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        results.add(row);
                    }
                    logger.debug("Query returned {} rows", results.size());
                }
            }
            return results;
        });
    }

    public int executeUpdate(String sql, Object... params) {
        logger.debug("Executing update: {}", sql);
        return execute(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                    logger.trace("Setting parameter {}: {}", i + 1, params[i]);
                }
                int affected = stmt.executeUpdate();
                logger.debug("Update affected {} rows", affected);
                return affected;
            }
        });
    }

    public Object callStoredProcedure(String procedureName, Object... params) {
        logger.debug("Calling stored procedure: {} with {} parameters", procedureName, params.length);
        return execute(conn -> {
            String template = PROC_CALL_TEMPLATES.getOrDefault(dbType, "{call %s(%s)}");
            String paramPlaceholders = String.join(",", java.util.Collections.nCopies(params.length, "?"));
            String callString = String.format(template, procedureName, paramPlaceholders);
            logger.debug("Prepared procedure call: {}", callString);
            
            try (var stmt = conn.prepareCall(callString)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                    logger.trace("Setting procedure parameter {}: {}", i + 1, params[i]);
                }
                
                boolean hasResultSet = stmt.execute();
                if (hasResultSet) {
                    logger.debug("Procedure returned a result set");
                    return stmt.getResultSet();
                } else {
                    int updateCount = stmt.getUpdateCount();
                    logger.debug("Procedure affected {} rows", updateCount);
                    return updateCount;
                }
            }
        });
    }

    public String getTestQuery() {
        return TEST_QUERIES.getOrDefault(dbType, "SELECT 1");
    }

    public List<String> parseSqlFile(File scriptFile) throws IOException {
        return SqlScriptParser.parse(scriptFile);
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                logger.debug("Closing database connection");
                connection.close();
                logger.info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection: {}", e.getMessage());
            throw new DatabaseConnectionException("Failed to close database connection", e);
        }
    }
}