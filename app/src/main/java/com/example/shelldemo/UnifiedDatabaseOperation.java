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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;

import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.exception.SqlParseException;
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
    
    private Map<Integer, String> parseScriptFile(File scriptFile) throws SQLException {
        try {
            Map<Integer, String> parsedScripts = SqlScriptParser.parseSqlFile(scriptFile);
            logger.debug("Found {} SQL statements in script", parsedScripts.size());
            return parsedScripts;
        } catch (SqlParseException e) {
            logger.error("Error parsing SQL file: {}", e.getMessage());
            throw new SQLException("Error executing SQL script", e);
        }
    }

    public String getExplainPlan(String sql) throws SQLException {
        return validator.getExplainPlan(connection, sql);
    }

    public void validateScript(File scriptFile, boolean showExplainPlan) throws SQLException {
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


    public int executeScript(File scriptFile) throws SQLException {
        logger.info("Executing SQL script: {}", scriptFile.getAbsolutePath());
        
        Map<Integer, String> parsedScripts = parseScriptFile(scriptFile);
        
        // Store original auto-commit setting
        boolean originalAutoCommit = connection.getAutoCommit();
        
        try {
            // Disable auto-commit for transaction management
            connection.setAutoCommit(false);
            int statementCount = executeStatements(parsedScripts);
            connection.commit();
            logger.info("Script execution completed successfully - {} statements executed", statementCount);
            System.out.println("Script execution completed successfully");
            return statementCount;
        } catch (SQLException e) {
            handleScriptExecutionError(e, scriptFile);
            throw e; // Re-throw the exception after handling
        } finally {
            restoreAutoCommit(originalAutoCommit);
        }
    }

    private int executeStatements(Map<Integer, String> parsedScripts) throws SQLException {
        int statementCount = 0;
        for (Map.Entry<Integer, String> entry : parsedScripts.entrySet()) {
            statementCount++;
            Integer statementNumber = entry.getKey();
            String sql = entry.getValue();
            logger.info("Executing SQL statement #{}: {}", statementNumber, sql);
            executeSingleStatement(sql);
        }
        return statementCount;
    }

    private void executeSingleStatement(String sql) throws SQLException {
        try {
            if (validator.isPLSQL(sql)) {
                // For PL/SQL, still use a Statement
                try (Statement stmt = connection.createStatement()) {
                    executePLSQLStatement(stmt, sql);
                }
            } else {
                executeRegularStatement(sql);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement: {}. Error: {}", sql, e.getMessage());
            throw e; // Rethrow to handle rollback
        }
    }

    private void executePLSQLStatement(Statement stmt, String sql) throws SQLException {
        logger.debug("Executing PL/SQL block: {}", sql);
        System.out.println(sql);
        boolean hasResultSet = stmt.execute(sql);
        if (hasResultSet) {
            logger.debug("PL/SQL block returned a result set");
        } else {
            int affected = stmt.getUpdateCount();
            logger.debug("PL/SQL block affected {} rows", affected);
        }
    }

    private void executeRegularStatement(String sql) {
        logger.debug("Executing SQL statement: {}", sql);
        String cleanedSql = stripTrailingSemicolon(sql);
        if (cleanedSql == null) {
            logger.warn("SQL statement is null, skipping execution.");
            return;
        }
        String trimmed = cleanedSql.trim().toLowerCase();
        if (trimmed.startsWith("select")) {
            List<Map<String, Object>> results = executeQuery(cleanedSql);
            logger.debug("Query returned {} rows", results.size());
            printQueryResults(results); // Print results as a table
        } else {
            int affected = executeUpdate(cleanedSql);
            logger.debug("SQL statement affected {} rows", affected);
        }
    }

    // Helper method to print query results as a table
    private void printQueryResults(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            logger.info("No results.");
            System.out.println(); // Ensures a blank line if needed
            return;
        }
        StringBuilder output = new StringBuilder();
        Map<String, Object> firstRow = results.get(0);
        String[] headers = firstRow.keySet().toArray(new String[0]);
        for (String header : headers) {
            output.append(String.format("%-20s", header));
        }
        output.append(System.lineSeparator());
        for (int i = 0; i < headers.length; i++) {
            output.append("--------------------");
        }
        output.append(System.lineSeparator());
        for (Map<String, Object> row : results) {
            for (String header : headers) {
                Object value = row.get(header);
                output.append(String.format("%-20s", value != null ? value.toString() : "(null)"));
            }
            output.append(System.lineSeparator());
        }
        // Add a blank line before and after for clarity
        String table = System.lineSeparator() + output.toString() + System.lineSeparator();
        if (logger.isInfoEnabled()) {
            logger.info(table);
        }
        System.out.print(table);
    }

    private void handleScriptExecutionError(SQLException e, File scriptFile) {
        try {
            connection.rollback();
            logger.warn("Transaction rolled back due to error");
        } catch (SQLException rollbackEx) {
            logger.error("Failed to roll back transaction after error", rollbackEx);
        }
        
        String errorMessage = String.format("Failed to execute script: %s", scriptFile.getName());
        logger.error(errorMessage, e);
        throw new DatabaseOperationException(
            errorMessage,
            e,
            DatabaseOperationException.ERROR_CODE_QUERY_FAILED
        );
    }

    private void restoreAutoCommit(boolean originalAutoCommit) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
            logger.warn("Failed to restore auto-commit setting: {}", e.getMessage());
        }
    }

    public int executeScriptWithBatching(File scriptFile, boolean printStatements) throws SQLException {
        logger.info("Executing SQL script with batching: {}", scriptFile.getAbsolutePath());
        
        List<String> statements = new ArrayList<>(parseScriptFile(scriptFile).values());
        logger.debug("Found {} SQL statements in script", statements.size());
        
        // Filter out PL/SQL statements - they should be executed via regular executeScript
        List<String> batchableStatements = statements.stream()
            .filter(sql -> !validator.isPLSQL(sql))
            .toList();
        
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

    /**
     * Removes a single trailing semicolon from the end of a SQL statement,
     * ignoring whitespace. Does not affect semicolons inside the statement.
     */
    public static String stripTrailingSemicolon(String sql) {
        if (sql == null) return null;
        String trimmed = sql.trim();
        // Only remove if the last non-whitespace character is a semicolon
        if (trimmed.endsWith(";")) {
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return sql;
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