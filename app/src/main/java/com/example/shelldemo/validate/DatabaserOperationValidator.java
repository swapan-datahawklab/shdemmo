package com.example.shelldemo.validate;

import com.example.shelldemo.connection.DatabaseConnectionFactory;
import com.example.shelldemo.parser.SqlScriptParser;
import com.example.shelldemo.exception.DatabaseOperationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.io.File;
import java.io.IOException;

public class DatabaserOperationValidator {
    private static final Logger logger = LogManager.getLogger(DatabaserOperationValidator.class);
    
    private final String dbType;
    private final DatabaseConnectionFactory connectionFactory;

    public DatabaserOperationValidator(String dbType) {
        this.dbType = dbType;
        this.connectionFactory = new DatabaseConnectionFactory();
    }

    /**
     * Main validation method that orchestrates the validation process
     */
    public void validateScript(Connection conn, String scriptPath, boolean showExplainPlan) throws SQLException, IOException {
        logger.info("Starting validation of script: {}", scriptPath);
        List<String> statements = SqlScriptParser.parse(new File(scriptPath));
        
        DatabaseOperationValidationContext context = new DatabaseOperationValidationContext(conn, showExplainPlan);
        
        for (String statement : statements) {
            validateStatement(statement.trim(), context);
        }
        
        logger.info("Validation completed successfully. {} statements validated.", 
            context.getStatementCount());
    }


    /**
     * Validates a single statement
     */
    private void validateStatement(String statement, DatabaseOperationValidationContext context) 
            throws SQLException {
        if (statement.isEmpty() || statement.startsWith("--")) {
            return;
        }

        int statementNum = context.incrementAndGetCount();
        logger.info("Validating statement {}", statementNum);

        try {
            if (isPLSQL(statement)) {
                validatePLSQLStatement(statement, context);
            } else {
                validateSQLStatement(statement, context);
            }
            logger.info("Statement {} is valid", statementNum);
        } catch (SQLException e) {
            String error = formatValidationError(e, statementNum);
            throw new DatabaseOperationException(error, e);
        }
    }

    /**
     * Validates PL/SQL specific statements
     */
    private void validatePLSQLStatement(String statement, DatabaseOperationValidationContext context) 
            throws SQLException {
        logger.debug("Validating PL/SQL statement");
        connectionFactory.validatePlsqlSyntax(
            context.getConnection(),
            statement, 
            dbType, 
            context.getCurrentUsername()
        );
    }

    /**
     * Validates regular SQL statements
     */
    private void validateSQLStatement(String statement, DatabaseOperationValidationContext context) 
            throws SQLException {
        if (context.isShowExplainPlan()) {
            String plan = getExplainPlan(context.getConnection(), statement);
            logger.info("Execution plan:\n{}", plan);
        } else {
            connectionFactory.validateSqlSyntax(
                context.getConnection(),
                statement, 
                dbType
            );
        }
    }

    /**
     * Determines if a statement is PL/SQL
     */
    public boolean isPLSQL(String sql) {
        String upperSql = sql.toUpperCase().trim();
        
        // Check for common PL/SQL block start keywords
        if (upperSql.startsWith("BEGIN") || upperSql.startsWith("DECLARE")) {
            return true;
        }
        
        // Check for CREATE OR REPLACE statements for PL/SQL objects
        if (upperSql.startsWith("CREATE") || upperSql.startsWith("CREATE OR REPLACE")) {
            return upperSql.contains("FUNCTION") || 
                   upperSql.contains("PROCEDURE") || 
                   upperSql.contains("PACKAGE") ||
                   upperSql.contains("TRIGGER") ||
                   upperSql.contains("TYPE");
        }
        
        // Check for anonymous PL/SQL blocks that might not start with BEGIN or DECLARE
        return upperSql.contains("BEGIN") && upperSql.contains("END;");
    }

    /**
     * Formats validation errors with context
     */
    private String formatValidationError(SQLException e, int statementNumber) {
        return String.format(
            "Invalid statement %d at position %d: %s", 
            statementNumber, 
            e.getErrorCode(), 
            e.getMessage()
        );
    }

    public String getExplainPlan(Connection conn, String sql) throws SQLException {
        String explainQuery = switch (dbType) {
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
        
        try (var stmt = conn.prepareStatement(explainQuery);
             var rs = stmt.executeQuery()) {
            StringBuilder plan = new StringBuilder();
            while (rs.next()) {
                plan.append(rs.getString(1)).append("\n");
            }
            return plan.toString();
        }
    }
}