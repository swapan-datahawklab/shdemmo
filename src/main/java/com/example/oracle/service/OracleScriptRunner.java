package com.example.oracle.service;

import com.example.oracle.datasource.DatabaseOperation;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * A script runner for Oracle that provides SQLPlus-like functionality and error handling.
 * Supports executing SQL scripts with proper error reporting and transaction management.
 */
public class OracleScriptRunner extends DatabaseOperation {
    private final boolean stopOnError;
    private final boolean autoCommit;
    private final boolean printStatements;

    /**
     * Creates a new OracleScriptRunner with the specified connection.
     * @param host Oracle database host
     * @param username Oracle database username
     * @param password Oracle database password
     * @param stopOnError true to stop on error, false to continue
     * @param autoCommit true to auto-commit, false to manage transactions manually
     * @param printStatements true to print statements, false to suppress
     */
    public OracleScriptRunner(String host, String username, String password, 
                            boolean stopOnError, boolean autoCommit, boolean printStatements) {
        super(host, username, password);
        this.stopOnError = stopOnError;
        this.autoCommit = autoCommit;
        this.printStatements = printStatements;
    }

    /**
     * Runs the specified SQL script.
     * @param scriptFile the SQL script file to execute
     * @throws Exception if there's an error executing the SQL
     */
    public void runScript(String scriptFile) throws Exception {
        List<String> statements = readStatements(scriptFile);
        
        execute(connection -> {
            connection.setAutoCommit(autoCommit);
            try (Statement stmt = connection.createStatement()) {
                for (String sql : statements) {
                    if (printStatements) {
                        System.out.println("Executing: " + sql);
                    }
                    try {
                        stmt.execute(sql);
                    } catch (Exception e) {
                        if (stopOnError) {
                            throw e;
                        }
                        System.err.println("Error executing statement: " + e.getMessage());
                    }
                }
                if (!autoCommit) {
                    connection.commit();
                }
            }
            return null;
        });
    }

    private List<String> readStatements(String scriptFile) throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                currentStatement.append(line).append(" ");
                if (line.endsWith(";")) {
                    statements.add(currentStatement.toString().trim());
                    currentStatement.setLength(0);
                }
            }
        }
        
        return statements;
    }
} 