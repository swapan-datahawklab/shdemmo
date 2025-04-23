package com.example.shelldemo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import picocli.CommandLine;
import com.example.shelldemo.config.ConfigurationHolder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

class UnifiedDatabaseRunnerTest {
    
    // Oracle connection parameters for HR schema
    private static final String ORACLE_HOST = "localhost";
    private static final int ORACLE_PORT = 1521;
    private static final String ORACLE_DATABASE = "FREEPDB1";
    private static final String ORACLE_USERNAME = "HR";
    private static final String ORACLE_PASSWORD = "HR";
    
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseRunnerTest.class);
    private static final String SEPARATOR = "========================================";
    private static final String SUCCESS_MARKER = "✓";
    private static final String FAILURE_MARKER = "✗";
    private static final String FAILED_SQL_DIR = "failed_sql_tests";
    
    @TempDir
    Path tempDir;

    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outputStream;
    private List<Path> sqlFilesForTest;
    
    @BeforeAll
    static void setupClass() throws Exception {
        logger.info("\n{}\nTest Suite Starting\n{}", SEPARATOR, SEPARATOR);
        
        // Initialize configuration
        ConfigurationHolder.getInstance();
        logger.info("Configuration initialized successfully");
        
        // Create directory for failed SQL files if it doesn't exist
        Path failedSqlDir = Paths.get(FAILED_SQL_DIR);
        if (!Files.exists(failedSqlDir)) {
            Files.createDirectory(failedSqlDir);
        }
    }
    
    @BeforeEach
    void setUp(TestInfo testInfo) throws Exception {
        // Initialize test resources
        assertDoesNotThrow(() -> {
            sqlFilesForTest = new ArrayList<>();
            outputStream = new ByteArrayOutputStream();
            
            // Store original streams
            originalOut = System.out;
            originalErr = System.err;
            
            // Set up test streams
            System.setOut(new PrintStream(outputStream));
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
        }, "Test setup should complete without errors");

        // Log test start using TestInfo
        assertNotNull(testInfo.getDisplayName(), "Test name should be available");
        logTestStart(testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown(TestInfo testInfo) throws Exception {
        String testName = testInfo.getDisplayName();
        boolean testSucceeded = true;
        
        // Restore original streams
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Process test output
        String output = outputStream.toString();
        if (!output.isEmpty()) {
            logger.debug("Test output: {}", output);
            
            // Verify no database errors occurred
            assertAll("Database operation validation",
                () -> assertFalse(output.contains("ERROR com.example.shelldemo.exception.DatabaseException"), 
                    "Test output contains database exceptions"),
                () -> assertFalse(output.contains("ORA-"), 
                    "Test output contains Oracle errors"),
                () -> assertFalse(output.contains("oracle.jdbc.OracleDatabaseException"), 
                    "Test output contains JDBC exceptions")
            );
        }
        
        // Save SQL files if we have any failures
        if (!testSucceeded) {
            saveSqlFilesOnFailure(testName);
        }
        
        // Always log test completion
        logTestFinish(testName, testSucceeded);
    }

    private void saveSqlFilesOnFailure(String testName) throws IOException {
        if (sqlFilesForTest.isEmpty()) {
            return;
        }

        // Create a timestamped directory for this test failure
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = testName.replaceAll("[^a-zA-Z0-9-_]", "_");
        Path failureDir = Paths.get(FAILED_SQL_DIR, safeName + "_" + timestamp);
        Files.createDirectories(failureDir);

        // Copy all SQL files used in the test
        for (Path sqlFile : sqlFilesForTest) {
            if (Files.exists(sqlFile)) {
                Path targetPath = failureDir.resolve(sqlFile.getFileName());
                Files.copy(sqlFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Saved failed test SQL file: {}", targetPath);
            }
        }
    }

    private void trackSqlFile(Path sqlFile) {
        sqlFilesForTest.add(sqlFile);
    }

    private void logTestStart(String testName) {
        logger.info("\n{}\nTest * {}\n{}", SEPARATOR, testName, SEPARATOR);
    }

    private void logTestFinish(String testName, boolean success) {
        String status = success ? "SUCCESS " + SUCCESS_MARKER : "FAIL " + FAILURE_MARKER;
        logger.info("\n{}\nTest * {} * {}\n{}", SEPARATOR, testName, status, SEPARATOR);
    }
    
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
    void testHRSchemaConnection() throws Exception {
        // Arrange
        Path sqlFile = tempDir.resolve("hr_connection_test.sql");
        Files.writeString(sqlFile, "SELECT * FROM employees WHERE rownum <= 5");
        trackSqlFile(sqlFile);
        
        // Act
        String[] args = createDefaultRunnerArgs(sqlFile.toString());
        int result = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        String output = outputStream.toString();
        
        // Assert
        assertAll(
            () -> assertEquals(0, result, "Runner should execute successfully"),
            () -> assertTrue(output.contains("EMPLOYEE_ID"), 
                "Output should contain employee data"),
            () -> assertFalse(output.contains("ORA-"), 
                "Output should not contain Oracle errors")
        );
    }

    @Test
    @DisplayName("Test query with real data")
    void testQueryWithRealData() throws Exception {
        // Arrange
        Path sqlFile = tempDir.resolve("hr_complex_query.sql");
        String complexQuery = 
            "SELECT d.department_name, COUNT(e.employee_id) as employee_count " +
            "FROM departments d JOIN employees e ON d.department_id = e.department_id " +
            "GROUP BY d.department_name";
        Files.writeString(sqlFile, complexQuery);
        trackSqlFile(sqlFile);

        // Act
        String[] args = createDefaultRunnerArgs(sqlFile.toString());
        int result = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        String output = outputStream.toString();

        // Assert
        assertAll("Complex query execution",
            () -> assertEquals(0, result, 
                "Runner should execute successfully"),
            () -> assertTrue(output.contains("DEPARTMENT_NAME"), 
                "Output should contain department name"),
            () -> assertTrue(output.contains("EMPLOYEE_COUNT"), 
                "Output should contain employee count"),
            () -> assertFalse(output.contains("ORA-"), 
                "Output should not contain Oracle errors"),
            () -> assertFalse(output.contains("Exception"), 
                "Output should not contain any exceptions")
        );
    }

    @Test
    @DisplayName("Test execute SQL script file")
    void testExecuteScriptFile() throws Exception {
        // Arrange
        Path sqlFile = tempDir.resolve("test_script.sql");
        String multipleQueries = String.join("\n",
            "SELECT employee_id, first_name, last_name FROM employees WHERE rownum <= 3;",
            "SELECT department_id, department_name FROM departments WHERE rownum <= 3;"
        );
        Files.writeString(sqlFile, multipleQueries);
        trackSqlFile(sqlFile);

        // Act
        String[] args = createDefaultRunnerArgs(sqlFile.toString());
        int result = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        String output = outputStream.toString();

        // Assert
        assertAll("Multiple SQL statements execution",
            () -> assertEquals(0, result, 
                "Runner should execute successfully"),
            () -> assertTrue(output.contains("EMPLOYEE_ID"), 
                "Output should contain employee data"),
            () -> assertTrue(output.contains("DEPARTMENT_ID"), 
                "Output should contain department data"),
            () -> assertFalse(output.contains("ORA-"), 
                "Output should not contain Oracle errors"),
            () -> assertFalse(output.contains("Exception"), 
                "Output should not contain any exceptions"),
            () -> assertTrue(output.contains("Script execution completed successfully"), 
                "Output should indicate successful script execution")
        );
    }

    @Test
    @DisplayName("Test SQL DDL script")
    void testSqlDdlScript() throws Exception {
        // Arrange
        Path sqlScriptPath = tempDir.resolve("test_ddl.sql");
        Path sourcePath = Path.of("/home/swapanc/code/shdemmo/app/src/test/resources/sql/create_employee_info_proc.sql");
        
        assertDoesNotThrow(() -> {
            // Copy SQL file and track it
            Files.copy(sourcePath, sqlScriptPath, StandardCopyOption.REPLACE_EXISTING);
            trackSqlFile(sqlScriptPath);
            
            // Verify file was copied correctly
            assertTrue(Files.exists(sqlScriptPath), "SQL script file should exist");
            assertTrue(Files.size(sqlScriptPath) > 0, "SQL script file should not be empty");
        }, "File operations should complete without errors");

        // Act
        String[] args = createDefaultRunnerArgs(sqlScriptPath.toString());
        int result = new CommandLine(new UnifiedDatabaseRunner())
            .setExecutionStrategy(new CommandLine.RunLast())
            .execute(args);
        String output = outputStream.toString();

        // Assert
        assertAll("DDL script execution",
            () -> assertEquals(0, result, 
                "SQL script execution should succeed"),
            () -> assertFalse(output.contains("ORA-"), 
                "Output should not contain Oracle errors"),
            () -> assertFalse(output.contains("Exception"), 
                "Output should not contain any exceptions"),
            () -> assertTrue(output.contains("Script execution completed successfully"), 
                "Output should indicate successful execution")
        );
    }

    @Test
    @DisplayName("Test Oracle Function Creation")
    void testOracleFunctionCreation() throws Exception {
        // Arrange
        Path sqlFile = Files.createTempFile(tempDir, "create_function", ".sql");
        String functionDefinition = 
            "CREATE OR REPLACE FUNCTION get_employee_info(p_emp_id IN NUMBER) " +
            "RETURN VARCHAR2 IS v_result VARCHAR2(100); BEGIN RETURN v_result; END;";
        
        assertDoesNotThrow(() -> {
            Files.writeString(sqlFile, functionDefinition);
            trackSqlFile(sqlFile);
            assertTrue(Files.exists(sqlFile), "SQL file should exist");
            assertTrue(Files.size(sqlFile) > 0, "SQL file should not be empty");
        }, "File operations should complete without errors");

        // Act
        String[] args = createDefaultRunnerArgs(sqlFile.toString());
        int result = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        String output = outputStream.toString();

        // Assert
        assertAll("Oracle function creation",
            () -> assertEquals(0, result, 
                "Function creation should succeed"),
            () -> assertTrue(output.contains("CREATE OR REPLACE FUNCTION"), 
                "Output should contain function creation statement"),
            () -> assertTrue(output.contains("Script execution completed successfully"), 
                "Output should indicate successful execution"),
            () -> assertFalse(output.contains("ERROR com.example.shelldemo.exception.DatabaseException"), 
                "Output should not contain database exceptions"),
            () -> assertFalse(output.contains("ORA-"), 
                "Output should not contain Oracle errors"),
            () -> assertFalse(output.contains("oracle.jdbc.OracleDatabaseException"), 
                "Output should not contain JDBC exceptions")
        );
    }

    @Test
    void testExample() throws Exception {
        // ... test setup ...
        
        String output = outputStream.toString();
        
        assertAll("Database operation validation",
            () -> assertFalse(output.contains("ERROR com.example.shelldemo.exception.DatabaseException"), 
                "Output should not contain database exceptions"),
            () -> assertFalse(output.contains("ORA-"), 
                "Output should not contain Oracle errors"),
            () -> assertFalse(output.contains("oracle.jdbc.OracleDatabaseException"), 
                "Output should not contain JDBC exceptions")
        );
    }

    /**
     * Extension method to ensure resources are properly closed
     */
    @ExtendWith(SystemStreamExtension.class)
    class SystemStreamExtension implements AfterEachCallback {
        @Override
        public void afterEach(ExtensionContext context) {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

}
