package com.example.shelldemo;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;

import java.io.IOException;
import java.io.File;

import com.example.shelldemo.model.base.AbstractDatabaseConnection;
import com.example.shelldemo.model.base.AbstractDatabaseOperation;
import com.example.shelldemo.model.domain.ConnectionConfig;
import com.example.shelldemo.util.SqlScriptParser;
import com.example.shelldemo.datasource.connections.OracleConnection;
import com.example.shelldemo.datasource.connections.PostgresConnection;
import com.example.shelldemo.datasource.connections.MySqlConnection;
import com.example.shelldemo.exception.DatabaseConnectionException;
/**
 * Abstract base class for unified database operations.
 * Provides common functionality for all database types.
 */
public class UnifiedDatabaseOperation extends AbstractDatabaseOperation {
    private final String dbType;
    private final ConnectionConfig config;

    public UnifiedDatabaseOperation(String dbType, ConnectionConfig config) {
        super(createConnection(config));
        this.dbType = dbType;
        this.config = config;
    }

    /**
     * Creates a database connection based on the provided configuration.
     *
     * @param config the connection configuration
     * @return a database connection
     * @throws SQLException if a database access error occurs
     */
    private static AbstractDatabaseConnection createConnection(ConnectionConfig config) {
        try {
            switch (config.getDbType().toLowerCase()) {
                case "oracle":
                    return new OracleConnection(config);
                case "postgresql":
                    return new PostgresConnection(config);
                case "mysql":
                    return new MySqlConnection(config);
                default:
                    throw new IllegalArgumentException("Unsupported database type: " + config.getDbType());
            }
        } catch (Exception e) {
            throw new DatabaseConnectionException("Failed to create database connection", e);
        }
    }

    public static UnifiedDatabaseOperation create(String dbType, ConnectionConfig config) {
        return new UnifiedDatabaseOperation(dbType, config);
    }

    @Override
    protected AbstractDatabaseConnection createDatabaseConnection(ConnectionConfig config) {
        return createConnection(config);
    }

    public String getDatabaseType() {
        return dbType;
    }

    public ConnectionConfig getConnectionConfig() {
        return config;
    }

    @Override
    public <T> T execute(ConnectionCallback<T> callback) throws SQLException {
        try (Connection conn = this.connection.getConnection()) {
            return callback.execute(conn);
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        return execute(conn -> {
            List<Map<String, Object>> results = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    int columnCount = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            // Use getColumnLabel for potential aliases, fallback to getName
                            String columnName = rs.getMetaData().getColumnLabel(i);
                            if (columnName == null || columnName.isEmpty()) {
                                columnName = rs.getMetaData().getColumnName(i);
                            }
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        results.add(row);
                    }
                }
            }
            return results;
        });
    }

    public int executeUpdate(String sql, Object... params) throws SQLException {
        // Using executeTransaction from base class might be safer if autoCommit is not desired by default
        return execute(conn -> { // Assuming autoCommit is true or handled by the connection
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                return stmt.executeUpdate();
            }
        });
    }

    public Object callStoredProcedure(String procedureName, Object... params) throws SQLException {
        return execute(conn -> {
            String callString = "{call " + procedureName + "(" + String.join(",", java.util.Collections.nCopies(params.length, "?")) + ")}";
            try (var stmt = conn.prepareCall(callString)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                boolean hasResultSet = stmt.execute();
                if (hasResultSet) {
                    return stmt.getResultSet();
                } else {
                    return stmt.getUpdateCount();
                }
            }
        });
    }

    /**
     * Parses a SQL script file into individual statements using SqlScriptParser.
     *
     * @param scriptFile The SQL script file to parse
     * @return List of SQL statements
     * @throws IOException If there's an error reading the file
     */
    public List<String> parseSqlFile(File scriptFile) throws IOException {
        return SqlScriptParser.parse(scriptFile);
    }

}