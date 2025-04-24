package com.example.shelldemo;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.exception.DatabaseConnectionException;
import com.example.shelldemo.exception.DatabaseOperationException;
import com.example.shelldemo.connection.ConnectionConfig;
import com.example.shelldemo.connection.DatabaseConnectionFactory;
import com.example.shelldemo.connection.CustomDriver;
import com.example.shelldemo.error.DatabaseErrorFormatter;
import com.example.shelldemo.config.ConfigurationHolder;
import com.example.shelldemo.validate.DatabaserOperationValidator;
import com.example.shelldemo.parser.storedproc.StoredProcedureParser;
import com.example.shelldemo.parser.storedproc.StoredProcedureInfo;
/**
 * Unified database operations class.
 * Provides common functionality for all database types using JDBC.
 */
public class UnifiedDatabaseOperation implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseOperation.class);
    

    private final String dbType;
    private final ConnectionConfig config;
    private final Connection connection;
    private final DatabaseConnectionFactory connectionFactory;
    private final DatabaseErrorFormatter errorFormatter;
    private final DatabaserOperationValidator validator;

    public static UnifiedDatabaseOperation create(String dbType, ConnectionConfig config) throws DatabaseOperationException {
        try {
            return new UnifiedDatabaseOperation(dbType, config);
        } catch (Exception e) {

            throw new DatabaseOperationException(
                "Failed to create database operation for type: " + dbType,
                e,
                DatabaseOperationException.ERROR_CODE_TRANSACTION_FAILED
            );
        }
    }

    public UnifiedDatabaseOperation(String dbType, ConnectionConfig config) {
        logger.debug("Creating UnifiedDatabaseOperation for type: {}", dbType);
        
        this.dbType = dbType.toLowerCase();
        this.config = config;
        this.connectionFactory = new DatabaseConnectionFactory();
        this.errorFormatter = new DatabaseErrorFormatter(dbType);
        
        try {
            connectionFactory.validateAndEnrichConfig(this.dbType, this.config);
        } catch (DatabaseConnectionException e) {
            throw new DatabaseConnectionException("Invalid database type: " + this.dbType, e);
        }
        
        this.connection = createConnection();
        this.validator = new DatabaserOperationValidator(dbType);
        logger.info("Database operation initialized successfully for {}", this.dbType);
    }

    private Connection createConnection() {
        try {
            return connectionFactory.getConnection(config);
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Failed to create database connection", e);
        }
    }
    private StoredProcedureInfo parseStoredProcedure(String procedureName) 
            throws DatabaseOperationException {
        try {
            return StoredProcedureParser.parse(procedureName);
        } catch (IllegalArgumentException e) {
            throw new DatabaseOperationException(
                "Failed to parse stored procedure: " + procedureName,
                e,
                DatabaseOperationException.ERROR_CODE_PROCEDURE_FAILED
            );
        }
    }
    
    private List<String> parseSqlFile(File scriptFile) 
            throws DatabaseOperationException {
        try {
            Map<Integer, String> parsedScripts = SqlScriptParser.parseSqlFile(scriptFile);
            List<String> scriptList = new ArrayList<>(parsedScripts.values());
            return scriptList;
        } catch (IOException e) {
            throw new DatabaseOperationException(
                "Failed to parse SQL file: " + scriptFile.getName(),
                e,
                DatabaseOperationException.ERROR_CODE_QUERY_FAILED
            );
        }
    }

    public String getExplainPlan(String sql) throws SQLException {
        return validator.getExplainPlan(connection, sql);
    }

    public void validateScript(File scriptFile, boolean showExplainPlan) throws IOException, SQLException {
        logger.info("Starting validation of {}", scriptFile.getName());
        validator.validateScript(connection, scriptFile.getPath(), showExplainPlan);
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection conn) throws SQLException;
    }

    public <T> T execute(SqlFunction<T> operation) {
        try {
            return operation.apply(connection);
        } catch (SQLException e) {
            throw new DatabaseOperationException(errorFormatter.formatError(e), e  );
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        logger.debug("Executing query: {}", sql);
        return execute(conn -> {
            List<Map<String, Object>> results = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
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
            StoredProcedureInfo procInfo = parseStoredProcedure(procedureName);
            String template = ConfigurationHolder.getInstance().getSqlTemplate(dbType, "procedure");
            String paramPlaceholders = String.join(",", java.util.Collections.nCopies(params.length, "?"));
            String callString = String.format(template, procInfo.getName(), paramPlaceholders);
            logger.debug("Prepared procedure call: {}", callString);
            
            try (var stmt = conn.prepareCall(callString)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
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


    public Object executeStoredProcedure(String procedureName, boolean isFunction, Object... params) {
        logger.info("Executing stored procedure: {}", procedureName);
        
        // Parse and validate the procedure name
        StoredProcedureInfo procInfo = parseStoredProcedure(procedureName);
        Object result = callStoredProcedure(procInfo.getName(), params);
        
        if (isFunction) {
            logger.info("Function execution successful, result: {}", result);
        } else {
            logger.info("Procedure execution successful");
        }
        
        return result;
    }


    public int executeScript(File scriptFile, boolean printStatements) throws SQLException {
        logger.info("Executing SQL script: {}", scriptFile.getAbsolutePath());
        
        Map<Integer, String> parsedScripts;
        try {
            parsedScripts = SqlScriptParser.parseSqlFile(scriptFile);
            logger.debug("Found {} SQL statements in script", parsedScripts.size());
        } catch (IOException e) {
            logger.error("Error parsing SQL file: {}", e.getMessage());
            throw new SQLException("Error executing SQL script", e);
        }

        // Store original auto-commit setting
        boolean originalAutoCommit = connection.getAutoCommit();
        
        try {
            // Disable auto-commit for transaction management
            connection.setAutoCommit(false);
            
            int statementCount = 0;
            try (Statement stmt = connection.createStatement()) {
                for (Map.Entry<Integer, String> entry : parsedScripts.entrySet()) {
                    Integer statementNumber = entry.getKey();
                    String sql = entry.getValue();
                    statementCount++;
                    
                    if (printStatements) {
                        logger.info("Executing SQL statement #{}: {}", statementNumber, sql);
                    } else {
                        logger.debug("Executing SQL statement #{} (length: {})", statementNumber, sql.length());
                    }
                    
                    try {
                        if (validator.isPLSQL(sql)) {
                            logger.debug("Executing PL/SQL block: {}", sql);
                            // Use execute() for PL/SQL blocks 
                            boolean hasResultSet = stmt.execute(sql);
                            if (hasResultSet) {
                                logger.debug("PL/SQL block returned a result set");
                            } else {
                                int affected = stmt.getUpdateCount();
                                logger.debug("PL/SQL block affected {} rows", affected);
                            }
                        } else {
                            // Use executeUpdate() for regular SQL statements
                            logger.debug("Executing SQL statement: {}", sql);
                            int affected = stmt.executeUpdate(sql);
                            logger.debug("SQL statement affected {} rows", affected);
                        }
                    } catch (SQLException e) {
                        logger.error("Error executing SQL statement: {}. Error: {}", sql, e.getMessage());
                        throw e; // Rethrow to handle rollback
                    }
                }
                
                // Commit all changes if everything succeeded
                connection.commit();
                logger.info("Script execution completed successfully - {} statements executed", statementCount);
                return statementCount;
            } catch (SQLException e) {
                // Roll back on any error
                try {
                    connection.rollback();
                    logger.warn("Transaction rolled back due to error");
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to roll back transaction after error", rollbackEx);
                }
                
                String errorMessage = String.format("Failed to execute script: %s at statement #%d",
                    scriptFile.getName(), statementCount);
                logger.error(errorMessage, e);
                throw new DatabaseOperationException(
                    errorMessage,
                    e,
                    DatabaseOperationException.ERROR_CODE_QUERY_FAILED
                );
            }
        } finally {
            // Restore original auto-commit setting
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                logger.warn("Failed to restore auto-commit setting: {}", e.getMessage());
            }
        }
    }

    public int executeScriptWithBatching(File scriptFile, boolean printStatements) throws SQLException {
        logger.info("Executing SQL script with batching: {}", scriptFile.getAbsolutePath());
        
        List<String> statements = parseSqlFile(scriptFile);
        logger.debug("Found {} SQL statements in script", statements.size());
        
        // Filter out PL/SQL statements - they should be executed via regular executeScript
        List<String> batchableStatements = statements.stream()
            .filter(sql -> !validator.isPLSQL(sql))
            .collect(Collectors.toList());
        
        if (batchableStatements.isEmpty()) {
            logger.info("No batchable statements found, use executeScript instead for PL/SQL blocks");
            return 0;
        }
        
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        
        try {
            int totalExecuted = executeBatch(batchableStatements, printStatements);
            connection.commit();
            logger.info("Batch execution completed successfully - {} statements executed", totalExecuted);
            return totalExecuted;
        } catch (Exception e) {
            connection.rollback();
            logger.error("Batch execution failed, rolling back transaction", e);
            throw new DatabaseOperationException(
                "Failed to execute script: " + scriptFile.getName(),
                e,
                DatabaseOperationException.ERROR_CODE_QUERY_FAILED
            );
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private int executeBatch(List<String> sqlStatements, boolean printStatements) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            for (String sql : sqlStatements) {
                if (printStatements) {
                    logger.info("Adding to batch: {}", sql);
                }
                stmt.addBatch(sql);
            }
            
            int[] results = stmt.executeBatch();
            int totalAffected = Arrays.stream(results)
                .filter(r -> r != Statement.SUCCESS_NO_INFO)
                .sum();
            
            logger.debug("Batch execution completed. Total rows affected: {}", totalAffected);
            return sqlStatements.size();
        }
    }

    public void loadDriverFromPath(String path) {
        logger.debug("Loading JDBC driver from path: {}", path);
        try {
            File driverFile = new File(path);
            if (!driverFile.exists()) {
                logger.error("Driver file not found: {}", path);
                return;
            }

            URL driverUrl = driverFile.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{driverUrl}, getClass().getClassLoader());
            logger.debug("Created URLClassLoader for driver path: {}", path);

            // Try to load the driver using ServiceLoader
            ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class, loader);
            for (Driver driver : drivers) {
                logger.info("Registering JDBC driver: {}", driver.getClass().getName());
                DriverManager.registerDriver(new CustomDriver(driver));
            }
            logger.debug("Driver loading completed successfully");
        } catch (Exception e) {
            throw new DatabaseConnectionException("Failed to load JDBC driver", e);
        }
    }



    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection: {}", e.getMessage());
            throw new DatabaseConnectionException("Failed to close database connection", e);
        }
    }
}