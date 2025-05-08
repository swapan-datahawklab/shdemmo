package com.example.shelldemo;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.parser.SqlScriptParser.StoredProcedureInfo;
import com.example.shelldemo.exception.DatabaseException;
import com.example.shelldemo.exception.DatabaseException.ErrorType;
import com.example.shelldemo.connection.DatabaseConnectionFactory;
import com.example.shelldemo.connection.ConnectionConfig;
import com.example.shelldemo.config.ConfigurationHolder;
import com.example.shelldemo.validate.DatabaserOperationValidator;

import com.example.shelldemo.sqlhandling.ResultSetProcessor;
import com.example.shelldemo.sqlhandling.DatabaseErrorHandler;
import com.example.shelldemo.sqlhandling.BatchExecutor;
import com.example.shelldemo.sqlhandling.StatementExecutor;
import com.example.shelldemo.sqlhandling.DatabaseConfig;
import com.example.shelldemo.sqlhandling.ResultSetStreamer;


/**
 * Unified database operations class.
 * Provides common functionality for all database types using JDBC.
 *
 * <p>Instances must be created via {@link UnifiedDatabaseOperationBuilder}.</p>
 */
public class UnifiedDatabaseOperation implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseOperation.class);
    
    private final Connection connection;
    private final StatementExecutor statementExecutor;
    private final BatchExecutor batchExecutor;
    private final DatabaseErrorHandler errorHandler;
    private final ResultSetProcessor resultSetProcessor;
    private final String dbType;

    private static final int DEFAULT_BATCH_SIZE = 1000;

    // Dedicated logger for result set output
    private static final Logger resultSetLogger = LogManager.getLogger("com.example.shelldemo.resultset");

    /**
     * Use UnifiedDatabaseOperationBuilder to construct instances.
     */
    private UnifiedDatabaseOperation(DatabaseConfig config) {
        this.dbType = config.getDbType().toLowerCase();
        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory();
        
        try {
            ConnectionConfig connConfig = new ConnectionConfig();
            connConfig.setDbType(config.getDbType());
            connConfig.setHost(config.getHost());
            connConfig.setPort(config.getPort());
            connConfig.setUsername(config.getUsername());
            connConfig.setPassword(config.getPassword());
            connConfig.setServiceName(config.getDatabase());
            connConfig.setConnectionType(config.getConnectionType());
            
            this.connection = connectionFactory.createConnection(connConfig);
            this.statementExecutor = new StatementExecutor(connection, new DatabaserOperationValidator(dbType));
            this.batchExecutor = new BatchExecutor(connection);
            this.errorHandler = new DatabaseErrorHandler(dbType);
            this.resultSetProcessor = new ResultSetProcessor();
            
            logger.info("Database operation initialized successfully for {}", this.dbType);
        } catch (SQLException e) {
            String errorMessage = "Failed to create database connection";
            logger.error(errorMessage, e);
            throw new DatabaseException(errorMessage, e, ErrorType.CONN_FAILED);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection conn) throws SQLException;
    }

    private StoredProcedureInfo parseStoredProcedure(String procedureName) {
        try {
            return SqlScriptParser.parseStoredProcedure(procedureName);
        } catch (IllegalArgumentException e) {
            throw new DatabaseException(
                "Failed to parse stored procedure: " + procedureName,
                e,
                ErrorType.PARSE_PROCEDURE
            );
        }
    }
    
    private Map<Integer, String> parseScriptFile(File scriptFile) {
        try {
            Map<Integer, String> parsedScripts = SqlScriptParser.parseSqlFile(scriptFile);
            logger.debug("Found {} SQL statements in script", parsedScripts.size());
            return parsedScripts;
        } catch (Exception e) {
            logger.error("Error parsing SQL file: {}", e.getMessage());
            throw new DatabaseException("Error parsing SQL script", e, ErrorType.PARSE_SQL);
        }
    }

    private <T> T execute(SqlFunction<T> operation) {
        try {
            return operation.apply(connection);
        } catch (SQLException e) {
            throw errorHandler.handleSQLException(e, "execute");
        }
    }

    private <T> T executeInTransaction(SqlFunction<T> work) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            T result = work.apply(connection);
            connection.commit();
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
                logger.warn("Transaction rolled back due to error");
            } catch (SQLException rollbackEx) {
                logger.error("Failed to roll back transaction after error", rollbackEx);
            }
            throw errorHandler.handleSQLException(e, "transaction");
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                logger.warn("Failed to restore auto-commit setting", e);
            }
        }
    }

    public List<Map<String, Object>> executeQuery(String sql, int pageSize, int pageNumber, Object... params) {
        logger.debug("Executing paginated query: {} with page size: {} and page number: {}", sql, pageSize, pageNumber);
        String paginatedSql = addPagination(sql, pageSize, pageNumber);
        return execute(conn -> {
            List<Map<String, Object>> results = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(paginatedSql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(resultSetProcessor.processRow(rs));
                    }
                    logger.debug("Query returned {} rows for page {}", results.size(), pageNumber);
                }
            }
            return results;
        });
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        return executeQuery(sql, DEFAULT_BATCH_SIZE, 1, params);
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
            String callString = String.format(template, procInfo.name(), paramPlaceholders);
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
        StoredProcedureInfo procInfo = parseStoredProcedure(procedureName);
        Object result = callStoredProcedure(procInfo.name(), params);
        
        if (isFunction) {
            logger.info("Function execution successful, result: {}", result);
        } else {
            logger.info("Procedure execution successful");
        }
        
        return result;
    }

    public void executeScript(File scriptFile) {
        executeScript(scriptFile, false); // Default: non-transactional
    }

    public void executeScript(File scriptFile, boolean transactional) {
            Map<Integer, String> parsedScripts = parseScriptFile(scriptFile);
        // Partition statements by type
        List<String> dmlStatements = new ArrayList<>();
        List<String> otherStatements = new ArrayList<>();
        for (String sql : parsedScripts.values()) {
            if (isDmlStatement(sql)) {
                dmlStatements.add(sql);
            } else {
                otherStatements.add(sql);
            }
        }
        // Execute DDL/PLSQL (always non-transactional)
        for (String sql : otherStatements) {
            logger.info("Executing non-transactional statement: {}", sql);
            executeSingleStatement(sql);
        }
        // Execute DML
        if (!dmlStatements.isEmpty()) {
            if (transactional) {
                logger.info("Executing DML statements in a transaction ({} statements)", dmlStatements.size());
                try {
            executeInTransaction(conn -> {
                        for (String sql : dmlStatements) {
                            executeSingleStatement(sql);
                        }
                return null;
            });
        } catch (SQLException e) {
                    logger.error("Failed to execute DML statements in transaction", e);
                    throw new DatabaseException("Failed to execute DML statements in transaction", e, ErrorType.OP_QUERY);
                }
            } else {
                logger.info("Executing DML statements non-transactionally ({} statements)", dmlStatements.size());
                for (String sql : dmlStatements) {
                    executeSingleStatement(sql);
                }
            }
        }
    }

    private boolean isDmlStatement(String sql) {
        String trimmed = sql.trim().toLowerCase();
        return trimmed.startsWith("insert") || trimmed.startsWith("update") || trimmed.startsWith("delete") || trimmed.startsWith("merge");
    }


    private void executeSingleStatement(String sql) {
        try {
            String sqlToExecute = statementExecutor.isPLSQL(sql)
                ? sql
                : stripTrailingSemicolon(sql);

            statementExecutor.executeStatement(sqlToExecute, (stmt, sqlStatement) -> {
                if (stmt.execute(sqlStatement)) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        List<Map<String, Object>> results = resultSetProcessor.processResultSet(rs);
                        printQueryResults(results);
                    }
                } else {
                    int affected = stmt.getUpdateCount();
                    logger.info("Statement affected {} rows", affected);
                }
            });
        } catch (SQLException e) {
            throw new DatabaseException("Failed to execute SQL statement", e, ErrorType.OP_QUERY);
        }
    }

    private void printQueryResults(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            logger.info("No results.");
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
        String table = System.lineSeparator() + output.toString() + System.lineSeparator();
        resultSetLogger.info(table); // Only prints to console, not to file logs
    }




    public void executeDmlScriptWithBatching(File scriptFile, boolean printStatements) {
        logger.info("Executing DML script with batching: {}", scriptFile.getAbsolutePath());
        
        List<String> statements = new ArrayList<>(parseScriptFile(scriptFile).values());
        logger.debug("Found {} SQL statements in script", statements.size());
        
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("SQL statements list cannot be empty");
        }

        List<String> batchableStatements = statements.stream()
            .filter(sql -> !statementExecutor.isPLSQL(sql))
            .toList();
        
        if (batchableStatements.isEmpty()) {
            throw new IllegalStateException("No DML statements found. Use executeScript instead for PL/SQL blocks");
        }
        
        try {
            executeInTransaction(conn -> {
                int totalExecuted = executeBatch(batchableStatements, printStatements);
                logger.info("Batch execution completed successfully - {} DML statements executed", totalExecuted);
                return null;
            });
        } catch (SQLException e) {
            logger.error("Failed to execute batch statements", e);
            throw new DatabaseException(
                "Failed to execute DML script: " + scriptFile.getName(),
                e,
                ErrorType.OP_QUERY
            );
        }
    }

    private int executeBatch(List<String> sqlStatements, boolean printStatements) throws SQLException {
        return batchExecutor.executeBatch(sqlStatements, printStatements);
    }

    public void executeQueryWithStreaming(String sql, ResultSetStreamer streamer, int batchSize, Object... params) throws SQLException, IOException {
        try (PreparedStatement stmt = prepareStatement(sql, params);
             ResultSet rs = stmt.executeQuery()) {
            streamer.stream(rs, batchSize);
        }
    }

    public StatementExecutor getStatementExecutor() {
        return statementExecutor;
    }

    private String addPagination(String sql, int pageSize, int pageNumber) {
        int offset = (pageNumber - 1) * pageSize;
        if (sql.toLowerCase().contains("limit") || sql.toLowerCase().contains("offset")) {
            throw new IllegalArgumentException("SQL query already contains LIMIT or OFFSET clause");
        }
        return String.format("%s LIMIT %d OFFSET %d", sql, pageSize, offset);
    }

    private PreparedStatement prepareStatement(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt;
        }
    }

    public static String stripTrailingSemicolon(String sql) {
        if (sql == null) return null;
        String trimmed = sql.trim();
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
            throw new DatabaseException("Failed to close database connection", e, ErrorType.CONN_FAILED);
        }
    }

    /**
     * Returns a new builder for UnifiedDatabaseOperation.
     */
    public static UnifiedDatabaseOperationBuilder builder() {
        return new UnifiedDatabaseOperationBuilder();
    }

    /**
     * Package-private factory for builder access.
     */
    static UnifiedDatabaseOperation create(DatabaseConfig config) {
        return new UnifiedDatabaseOperation(config);
    }
}