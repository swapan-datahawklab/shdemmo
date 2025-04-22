package com.example.shelldemo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;


public class UnifiedDatabaseRunnerTest {
    
    // Oracle connection parameters for HR schema
    private static final String ORACLE_HOST = "localhost";
    private static final int ORACLE_PORT = 1521;
    private static final String ORACLE_DATABASE = "FREEPDB1";
    private static final String ORACLE_USERNAME = "HR";
    private static final String ORACLE_PASSWORD = "HR";
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedDatabaseRunnerTest.class);
    
    @TempDir
    Path tempDir;
    
    /**
     * Helper method to create a standard set of arguments for UnifiedDatabaseRunner
     */
    private String[] createDefaultRunnerArgs(String target) {
        return new String[] {
            "--type", "oracle",
            "--connection-type", "thin",
            "--host", ORACLE_HOST,
            "--port", String.valueOf(ORACLE_PORT),
            "--username", ORACLE_USERNAME,
            "--password", ORACLE_PASSWORD, 
            "--database", ORACLE_DATABASE,
            "--stop-on-error",
            "--print-statements",
            target
        };
    }
    
    // Removed unused method setRequiredFields(UnifiedDatabaseRunner runner)

    @Test
    @DisplayName("Test connection to HR schema")
    public void testHRSchemaConnection() throws Exception {
        // Create a temporary SQL file with a simple query
        Path sqlFile = tempDir.resolve("hr_connection_test.sql");
        Files.writeString(sqlFile, "SELECT * FROM employees WHERE rownum <= 5");
        
        // Capture the output of the command
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            
            // Run the UnifiedDatabaseRunner via command line
            String[] args = {
                "--type", "oracle",
                "--connection-type", "thin",
                "--host", ORACLE_HOST,
                "--port", String.valueOf(ORACLE_PORT),
                "--username", ORACLE_USERNAME,
                "--password", ORACLE_PASSWORD,
                "--database", ORACLE_DATABASE,
                "--stop-on-error",
                "--print-statements",
                sqlFile.toFile().getAbsolutePath()
            };
            
            int result = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
            
            // Verify successful execution
            assertEquals(0, result, "Runner should execute successfully");
            
            // Verify output contains expected data
            String output = outputStream.toString();
            assertTrue(output.contains("EMPLOYEE_ID"), "Output should contain employee data");
            logger.info("Successfully verified connection to HR schema");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Test query with real data")
    public void testQueryWithRealData() throws Exception {
        // Create a temporary SQL file with a more complex query
        Path sqlFile = tempDir.resolve("hr_complex_query.sql");
        Files.writeString(sqlFile,
            "SELECT d.department_name, COUNT(e.employee_id) as employee_count " +
            "FROM departments d JOIN employees e ON d.department_id = e.department_id " +
            "GROUP BY d.department_name");
            
        // Capture the output of the command
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            
            // Run the UnifiedDatabaseRunner via command line
            String[] args = {
                "--type", "oracle",
                "--connection-type", "thin",
                "--host", ORACLE_HOST,
                "--port", String.valueOf(ORACLE_PORT),
                "--username", ORACLE_USERNAME,
                "--password", ORACLE_PASSWORD,
                "--database", ORACLE_DATABASE,
                "--stop-on-error",
                "--print-statements",
                sqlFile.toFile().getAbsolutePath()
            };
            
            int result = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
            
            // Verify successful execution
            assertEquals(0, result, "Runner should execute successfully");
            
            // Verify output contains expected data
            String output = outputStream.toString();
            assertTrue(output.contains("DEPARTMENT_NAME"), "Output should contain department name");
            assertTrue(output.contains("EMPLOYEE_COUNT"), "Output should contain employee count");
            logger.info("Successfully queried departments with employee counts");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Test execute SQL script file")
    public void testExecuteScriptFile() throws Exception {
        // Create a temporary SQL script
        Path sqlFile = tempDir.resolve("test_script.sql");
        Files.writeString(sqlFile, 
            "SELECT employee_id, first_name, last_name FROM employees WHERE rownum <= 3;\n" +
            "SELECT department_id, department_name FROM departments WHERE rownum <= 3;\n");
        
        // Execute the script using command line
        String[] args = createDefaultRunnerArgs(sqlFile.toFile().getAbsolutePath());
        int result = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        
        // Verify successful execution
        assertEquals(0, result, "Runner should execute successfully");
        logger.info("Successfully executed SQL script");
    }

    @Test
    @DisplayName("Test execute HR schema stored procedure")
    public void testExecuteStoredProcedure() throws Exception {
        // Path to the SQL files
        String createProcPath = tempDir.resolve("create_proc.sql").toString();
        String dropProcPath = tempDir.resolve("drop_proc.sql").toString();
        
        // Copy SQL files to temp directory for the test
        Files.copy(
            Path.of("/home/swapanc/code/shdemmo/app/src/test/resources/sql/create_employee_info_proc.sql"),
            Path.of(createProcPath),
            StandardCopyOption.REPLACE_EXISTING
        );
        Files.copy(
            Path.of("/home/swapanc/code/shdemmo/app/src/test/resources/sql/drop_employee_info_proc.sql"),
            Path.of(dropProcPath),
            StandardCopyOption.REPLACE_EXISTING
        );
        
        try {
            // First create the stored procedure using UnifiedDatabaseRunner
            String[] createArgs = createDefaultRunnerArgs(createProcPath);
            int createResult = new CommandLine(new UnifiedDatabaseRunner()).execute(createArgs);
            assertEquals(0, createResult, "Procedure creation should succeed");
            
            // Now execute the stored procedure using command line
            String[] callProcArgs = {
                "--type", "oracle",
                "--connection-type", "thin",
                "--host", ORACLE_HOST,
                "--port", String.valueOf(ORACLE_PORT),
                "--username", ORACLE_USERNAME,
                "--password", ORACLE_PASSWORD,
                "--database", ORACLE_DATABASE,
                "--stop-on-error",
                "--input-params", "p_emp_id:NUMBER:100",
                "--output-params", "p_result:VARCHAR2",
                "get_employee_info"
            };
            int result = new CommandLine(new UnifiedDatabaseRunner()).execute(callProcArgs);
            
            // Verify successful execution
            assertEquals(0, result, "Runner should execute procedure successfully");
            logger.info("Successfully executed stored procedure");
        } finally {
            // Clean up by dropping the procedure using UnifiedDatabaseRunner
            String[] dropArgs = createDefaultRunnerArgs(dropProcPath);
            new CommandLine(new UnifiedDatabaseRunner()).execute(dropArgs);
        }
    }

    @Test
    @DisplayName("Test execute HR schema function")
    public void testExecuteFunction() throws Exception {
        // First create a SQL file with function creation
        Path createFunctionSql = tempDir.resolve("create_function.sql");
        Files.writeString(createFunctionSql, 
            "CREATE OR REPLACE FUNCTION get_department_name(p_dept_id IN NUMBER) RETURN VARCHAR2 AS " +
            "  v_name VARCHAR2(30); " +
            "BEGIN " +
            "  SELECT department_name INTO v_name " +
            "  FROM departments WHERE department_id = p_dept_id; " +
            "  RETURN v_name; " +
            "END;");
        
        // Create a SQL file for function deletion
        Path dropFunctionSql = tempDir.resolve("drop_function.sql");
        Files.writeString(dropFunctionSql, "DROP FUNCTION get_department_name");
        
        try {
            // Execute function creation via the runner
            String[] createArgs = {
                "--type", "oracle",
                "--connection-type", "thin",
                "--host", ORACLE_HOST,
                "--port", String.valueOf(ORACLE_PORT),
                "--username", ORACLE_USERNAME,
                "--password", ORACLE_PASSWORD,
                "--database", ORACLE_DATABASE,
                "--stop-on-error", "true",
                createFunctionSql.toFile().getAbsolutePath()
            };
            int createResult = new CommandLine(new UnifiedDatabaseRunner()).execute(createArgs);
            assertEquals(0, createResult, "Function creation should succeed");
            
            // Now call the function via the runner
            String[] callArgs = {
                "--type", "oracle",
                "--connection-type", "thin",
                "--host", ORACLE_HOST,
                "--port", String.valueOf(ORACLE_PORT),
                "--username", ORACLE_USERNAME,
                "--password", ORACLE_PASSWORD,
                "--database", ORACLE_DATABASE,
                "--stop-on-error", "true",
                "--is-function", "true",
                "--return-type", "VARCHAR2",
                "--input-params", "p_dept_id:NUMBER:10",
                "get_department_name"
            };
            
            int callResult = new CommandLine(new UnifiedDatabaseRunner()).execute(callArgs);
            
            // Verify successful execution
            assertEquals(0, callResult, "Runner should execute function successfully");
            logger.info("Successfully executed function");
        } finally {
            // Clean up by dropping the function, also using the runner
            String[] dropArgs = {
                "--type", "oracle",
                "--connection-type", "thin",
                "--host", ORACLE_HOST,
                "--port", String.valueOf(ORACLE_PORT),
                "--username", ORACLE_USERNAME,
                "--password", ORACLE_PASSWORD,
                "--database", ORACLE_DATABASE,
                "--stop-on-error", "true",
                dropFunctionSql.toFile().getAbsolutePath()
            };
            new CommandLine(new UnifiedDatabaseRunner()).execute(dropArgs);
        }
    }
}
