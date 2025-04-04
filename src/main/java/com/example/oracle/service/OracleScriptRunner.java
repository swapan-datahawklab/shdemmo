package com.example.oracle.service;

import java.io.*;
import java.sql.*;

/**
 * A script runner for Oracle that provides SQLPlus-like functionality and error handling.
 * Supports executing SQL scripts with proper error reporting and transaction management.
 */
public class OracleScriptRunner implements AutoCloseable {
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
     * Sets whether to stop execution on first error.
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
     * Runs the specified SQL script.
     * @param scriptFile the SQL script file to execute
     * @throws IOException if there's an error reading the file
     * @throws SQLException if there's an error executing the SQL
     */
    public void runScript(File scriptFile) throws IOException, SQLException {
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(autoCommit);
                executeScript(reader);
                if (!autoCommit) {
                    connection.commit();
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private void executeScript(BufferedReader reader) throws IOException, SQLException {
        StringBuilder command = new StringBuilder();
        String line;
        int lineNumber = 0;
        
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmedLine = line.trim();
            
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                continue;
            }
            
            command.append(line).append("\n");
            
            if (trimmedLine.endsWith(";")) {
                String sql = command.toString().trim();
                
                if (printStatements) {
                    System.out.println(sql);
                }
                
                try {
                    executeStatement(sql);
                } catch (SQLException e) {
                    System.err.printf("Error executing statement at line %d:%n", lineNumber);
                    System.err.println(sql);
                    System.err.println("Error: " + formatOracleError(e));
                    
                    if (stopOnError) {
                        throw e;
                    }
                }
                
                command.setLength(0);
            }
        }
    }

    private void executeStatement(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            boolean isQuery = stmt.execute(sql);
            if (isQuery) {
                try (ResultSet rs = stmt.getResultSet()) {
                    printResultSet(rs);
                }
            }
        }
    }

    private void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Print column headers
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) System.out.print("\t");
            System.out.print(metaData.getColumnLabel(i));
        }
        System.out.println();
        
        // Print separator line
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) System.out.print("\t");
            System.out.print("-".repeat(metaData.getColumnLabel(i).length()));
        }
        System.out.println();
        
        // Print data rows
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) System.out.print("\t");
                System.out.print(rs.getString(i));
            }
            System.out.println();
        }
    }

    private String formatOracleError(SQLException e) {
        StringBuilder error = new StringBuilder();
        error.append("ORA-").append(String.format("%05d", e.getErrorCode()))
             .append(": ").append(e.getMessage());
        
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

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
} 