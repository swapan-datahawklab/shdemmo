package com.example.shelldemo.datasource;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.lang.StringBuilder;

import com.example.shelldemo.model.AbstractDatabaseConnection;
import com.example.shelldemo.model.AbstractDatabaseOperation;
import com.example.shelldemo.model.ConnectionConfig;

/**
 * Abstract base class for unified database operations.
 * Provides common functionality for all database types.
 */
public class UnifiedDatabaseOperation extends AbstractDatabaseOperation {
    private final String dbType;
    private final ConnectionConfig config;
    
    private UnifiedDatabaseOperation(String dbType, ConnectionConfig config, AbstractDatabaseConnection connection) {
        super(connection);
        this.dbType = dbType;
        this.config = config;
    }
    
    public static UnifiedDatabaseOperation create(String dbType, ConnectionConfig config) throws SQLException {
        if (dbType == null || dbType.trim().isEmpty()) {
            throw new SQLException("Database type cannot be null or empty");
        }
        String validatedDbType = dbType.trim().toLowerCase();
        
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());
        
        String url = String.format(getUrlFormat(validatedDbType), 
            config.getHost(), 
            config.getPort(), 
            config.getServiceName());
            
        AbstractDatabaseConnection connection = new AbstractDatabaseConnection(config) {
            @Override
            protected Connection createConnection() throws SQLException {
                return DriverManager.getConnection(url, props);
            }
            
            @Override
            protected Connection createConnection(ConnectionConfig config) throws SQLException {
                return DriverManager.getConnection(url, props);
            }
        };
        
        return new UnifiedDatabaseOperation(validatedDbType, config, connection);
    }
    
    @Override
    protected AbstractDatabaseConnection createDatabaseConnection(ConnectionConfig config) throws SQLException {
        return create(dbType, config).connection;
    }
    
    private static String getUrlFormat(String dbType) {
        switch (dbType) {
            case "oracle":
                return "jdbc:oracle:thin:@//%s:%d/freepdb1?SERVICE_NAME=freepdb1";
            case "postgresql":
                return "jdbc:postgresql://%s:%d/%s";
            case "mysql":
                return "jdbc:mysql://%s:%d/%s";
            case "sqlserver":
                return "jdbc:sqlserver://%s:%d;databaseName=%s";
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
    
    public String getDatabaseType() {
        return dbType;
    }
    
    public ConnectionConfig getConnectionConfig() {
        return config;
    }
    
    @Override
    public <T> T execute(ConnectionCallback<T> callback) throws SQLException {
        Connection connection = null;
        try {
            connection = createDatabaseConnection(config).getConnection();
            return callback.execute(connection);
        } finally {
            if (connection != null) {
                connection.close();
            }
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
                            String columnName = rs.getMetaData().getColumnName(i);
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
        return execute(conn -> {
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
            try (var stmt = conn.prepareCall(procedureName)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.execute();
                return stmt.getObject(1);
            }
        });
    }
    
    public List<String> parseSqlFile(File scriptFile) throws IOException {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inMultilineComment = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }
                
                // Handle multiline comments
                if (!inMultilineComment && line.startsWith("/*")) {
                    inMultilineComment = true;
                    continue;
                }
                if (inMultilineComment) {
                    if (line.contains("*/")) {
                        inMultilineComment = false;
                    }
                    continue;
                }
                
                // Skip single-line comments
                if (line.startsWith("--") || line.startsWith("//")) {
                    continue;
                }
                
                // Remove inline comments
                int commentStart = line.indexOf("--");
                if (commentStart >= 0) {
                    line = line.substring(0, commentStart).trim();
                }
                
                currentStatement.append(line).append("\n");
                
                // Check for statement termination
                if (line.endsWith(";")) {
                    String stmt = currentStatement.toString().trim();
                    // Remove trailing semicolon and add to list if not empty
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    currentStatement.setLength(0);
                }
            }
            
            // Handle last statement if it exists (without semicolon)
            String lastStmt = currentStatement.toString().trim();
            if (!lastStmt.isEmpty()) {
                statements.add(lastStmt);
            }
        }
        
        return statements;
    }
} 