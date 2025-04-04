package com.example.oracle.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Example demonstrating how to use the OracleScriptRunner.
 * This class provides a service-oriented approach to running SQL scripts.
 */
public class ScriptRunnerExample {
    private static final Logger logger = LoggerFactory.getLogger(ScriptRunnerExample.class);

    /**
     * Executes an SQL script using the OracleScriptRunner.
     *
     * @param host Database host (format: host:port/service)
     * @param username Database username
     * @param password Database password
     * @param scriptPath Path to the SQL script file
     * @param stopOnError Whether to stop execution on error
     * @param autoCommit Whether to auto-commit transactions
     * @param printStatements Whether to print executed statements
     * @throws SQLException if a database access error occurs
     */
    public void executeScript(
            String host,
            String username,
            String password,
            String scriptPath,
            boolean stopOnError,
            boolean autoCommit,
            boolean printStatements) throws SQLException {
        
        String url = "jdbc:oracle:thin:@" + host;
        File scriptFile = new File(scriptPath);
        
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Script file does not exist: " + scriptPath);
        }

        logger.info("Connecting to database at {}", host);
        try (Connection conn = DriverManager.getConnection(url, username, password);
             OracleScriptRunner runner = new OracleScriptRunner(conn)) {
            
            // Configure the runner
            runner.setStopOnError(stopOnError)
                  .setAutoCommit(autoCommit)
                  .setPrintStatements(printStatements);

            logger.info("Executing script: {}", scriptPath);
            runner.runScript(scriptFile);
            logger.info("Script execution completed successfully");

        } catch (SQLException e) {
            logger.error("Database error while executing script: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while executing script", e);
            throw new RuntimeException("Failed to execute script: " + e.getMessage(), e);
        }
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: ScriptRunnerExample <host:port/service> <username> <password> <script_file>");
            System.exit(1);
        }

        ScriptRunnerExample example = new ScriptRunnerExample();
        try {
            example.executeScript(
                args[0],          // host
                args[1],          // username
                args[2],          // password
                args[3],          // scriptPath
                true,            // stopOnError
                false,           // autoCommit
                true             // printStatements
            );
        } catch (Exception e) {
            logger.error("Error executing script", e);
            System.exit(1);
        }
    }
} 