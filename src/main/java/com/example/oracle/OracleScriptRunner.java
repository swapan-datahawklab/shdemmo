package com.example.oracle;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A SQL script runner for Oracle that provides SQLPlus-like functionality and error handling.
 * Supports running SQL scripts with proper error reporting and statement delimiting.
 */
public class OracleScriptRunner implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(OracleScriptRunner.class.getName());
    private static final Pattern STATEMENT_DELIMITER = Pattern.compile(";\\s*$", Pattern.MULTILINE);
    private static final Pattern PLSQL_BLOCK_START = Pattern.compile("^\\s*(CREATE|DECLARE|BEGIN)\\s", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLSQL_BLOCK_END = Pattern.compile("^\\s*END;\\s*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    
    private final Connection connection;
    private boolean stopOnError = true;
    private boolean autoCommit = false;
    private boolean printStatements = false;
    
    /**
     * Creates a new OracleScriptRunner with the specified connection.
     * @param connection JDBC connection to Oracle database
     */
    public OracleScriptRunner(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }
    
    /**
     * Sets whether execution should stop on first error.
     * @param stopOnError true to stop on error, false to continue
     * @return this instance for method chaining
     */
    public OracleScriptRunner setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
        return this;
    }
    
    /**
     * Sets whether to auto-commit after each statement.
     * @param autoCommit true to auto-commit, false to manage transactions manually
     * @return this instance for method chaining
     */
    public OracleScriptRunner setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
        return this;
    }
    
    /**
     * Sets whether to print executed statements.
     * @param printStatements true to print statements, false to suppress
     * @return this instance for method chaining
     */
    public OracleScriptRunner setPrintStatements(boolean printStatements) {
        this.printStatements = printStatements;
        return this;
    }
    
    /**
     * Runs a SQL script from a file.
     * @param scriptFile the SQL script file to execute
     * @throws IOException if there's an error reading the file
     * @throws SQLException if there's an error executing the SQL
     */
    public void runScript(File scriptFile) throws IOException, SQLException {
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            runScript(reader);
        }
    }
    
    /**
     * Runs a SQL script from a reader.
     * @param reader the reader containing SQL statements
     * @throws IOException if there's an error reading the script
     * @throws SQLException if there's an error executing the SQL
     */
    public void runScript(Reader reader) throws IOException, SQLException {
        StringBuilder script = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        
        while ((line = bufferedReader.readLine()) != null) {
            script.append(line).append("\n");
        }
        
        List<String> statements = parseStatements(script.toString());
        boolean originalAutoCommit = connection.getAutoCommit();
        
        try {
            connection.setAutoCommit(autoCommit);
            
            for (String statement : statements) {
                executeStatement(statement);
                
                if (!autoCommit && !connection.getAutoCommit()) {
                    connection.commit();
                }
            }
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }
    
    /**
     * Parses individual SQL statements from a script.
     * @param script the complete SQL script
     * @return list of individual SQL statements
     */
    private List<String> parseStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        String[] lines = script.split("\n");
        boolean inPlSqlBlock = false;
        
        for (String line : lines) {
            // Skip comments and empty lines
            if (line.trim().isEmpty() || line.trim().startsWith("--")) {
                continue;
            }
            
            currentStatement.append(line).append("\n");
            
            if (!inPlSqlBlock) {
                if (PLSQL_BLOCK_START.matcher(line.trim()).find()) {
                    inPlSqlBlock = true;
                    continue;
                }
                
                if (STATEMENT_DELIMITER.matcher(line).find()) {
                    statements.add(currentStatement.toString().trim());
                    currentStatement.setLength(0);
                }
            } else {
                if (PLSQL_BLOCK_END.matcher(line).find()) {
                    inPlSqlBlock = false;
                    statements.add(currentStatement.toString().trim());
                    currentStatement.setLength(0);
                }
            }
        }
        
        // Add any remaining statement
        String remaining = currentStatement.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }
        
        return statements;
    }
    
    /**
     * Executes a single SQL statement with error handling.
     * @param statement the SQL statement to execute
     * @throws SQLException if there's an error executing the SQL
     */
    private void executeStatement(String statement) throws SQLException {
        if (printStatements) {
            System.out.println("\nExecuting: " + statement);
        }
        
        try (Statement stmt = connection.createStatement()) {
            boolean isQuery = stmt.execute(statement);
            
            if (isQuery) {
                try (ResultSet rs = stmt.getResultSet()) {
                    printResultSet(rs);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                System.out.println(updateCount + " row(s) affected");
            }
        } catch (SQLException e) {
            String errorMessage = formatOracleError(e);
            System.err.println(errorMessage);
            
            if (stopOnError) {
                throw e;
            } else {
                LOGGER.log(Level.WARNING, "Error executing statement: " + statement, e);
            }
        }
    }
    
    /**
     * Formats Oracle errors in a SQLPlus-like format.
     * @param e the SQLException to format
     * @return formatted error message
     */
    private String formatOracleError(SQLException e) {
        StringBuilder error = new StringBuilder();
        error.append("ERROR at line ")
             .append(getLineNumber(e))
             .append(":\n");
        
        if (e.getErrorCode() > 0) {
            error.append("ORA-").append(String.format("%05d", e.getErrorCode()))
                 .append(": ");
        }
        
        error.append(e.getMessage());
        
        // Handle chained exceptions
        SQLException nextException = e.getNextException();
        while (nextException != null) {
            error.append("\nCaused by: ORA-")
                 .append(String.format("%05d", nextException.getErrorCode()))
                 .append(": ")
                 .append(nextException.getMessage());
            nextException = nextException.getNextException();
        }
        
        return error.toString();
    }
    
    /**
     * Attempts to extract line number from Oracle error message.
     * @param e the SQLException to analyze
     * @return extracted line number or 1 if not found
     */
    private int getLineNumber(SQLException e) {
        Pattern linePattern = Pattern.compile("line\\s+(\\d+)");
        Matcher matcher = linePattern.matcher(e.getMessage());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }
    
    /**
     * Prints a result set in a formatted table.
     * @param rs the result set to print
     * @throws SQLException if there's an error accessing the result set
     */
    private void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Print column headers
        for (int i = 1; i <= columnCount; i++) {
            System.out.printf("%-20s", metaData.getColumnLabel(i));
        }
        System.out.println("\n" + "-".repeat(columnCount * 20));
        
        // Print rows
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                System.out.printf("%-20s", rs.getString(i));
            }
            System.out.println();
        }
        System.out.println();
    }
    
    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
} 