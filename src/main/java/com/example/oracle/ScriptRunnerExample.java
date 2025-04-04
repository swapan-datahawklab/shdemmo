package com.example.oracle;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Example demonstrating how to use the OracleScriptRunner.
 */
public class ScriptRunnerExample {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: ScriptRunnerExample <host:port/service> <username> <password> <script_file>");
            System.exit(1);
        }

        String url = "jdbc:oracle:thin:@" + args[0];
        String username = args[1];
        String password = args[2];
        String scriptPath = args[3];

        try (Connection conn = DriverManager.getConnection(url, username, password);
             OracleScriptRunner runner = new OracleScriptRunner(conn)) {
            
            // Configure the runner
            runner.setStopOnError(true)
                  .setAutoCommit(false)
                  .setPrintStatements(true);

            // Run the script
            System.out.println("Executing script: " + scriptPath);
            runner.runScript(new File(scriptPath));
            System.out.println("Script execution completed successfully");

        } catch (Exception e) {
            System.err.println("Error executing script:");
            e.printStackTrace();
            System.exit(1);
        }
    }
} 