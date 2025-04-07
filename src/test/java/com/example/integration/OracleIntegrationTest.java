package com.example.integration;

import com.example.cli.UnifiedDatabaseRunner;
import picocli.CommandLine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class OracleIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    private static final OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withPassword("oracle");

    private static Path tempDir;
    private UnifiedDatabaseRunner runner;

    @BeforeAll
    static void setupDatabase() throws Exception {
        tempDir = Files.createTempDirectory("oracle-test");
        oracle.start();

        // Execute initialization scripts in order
        String[] scriptNames = {
            "1_create_hr_userl.sql",
            "2_create_hr_tables.sql",
            "3_populate.sql",
            "4_others.sql"
        };

        for (String scriptName : scriptNames) {
            String scriptPath = "oracle_init_scripts/sql/" + scriptName;
            String containerScriptPath = "/tmp/" + scriptName;

            oracle.copyFileToContainer(
                MountableFile.forClasspathResource(scriptPath),
                containerScriptPath
            );

            var result = oracle.execInContainer(
                "sqlplus",
                "sys/oracle@//localhost:1521/XE",
                "AS SYSDBA",
                "@" + containerScriptPath,
                "exit"
            );

            System.out.println("Script " + scriptName + " output:\n" + result.getStdout());
            if (!result.getStderr().isEmpty()) {
                System.err.println("Script " + scriptName + " errors:\n" + result.getStderr());
            }
        }
    }

    @BeforeEach
    void setUp() {
        runner = new UnifiedDatabaseRunner();
    }

    @AfterAll
    static void cleanup() throws Exception {
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // Log error
                }
            });
    }

    @Test
    void testBasicScriptExecution() throws Exception {
        File scriptFile = createTestScript("""
            CREATE TABLE test_table (
                id NUMBER PRIMARY KEY,
                name VARCHAR2(100)
            );
            INSERT INTO test_table VALUES (1, 'Test');
            """);

        String[] args = buildBaseArgs();
        args[args.length - 1] = scriptFile.getAbsolutePath();

        assertEquals(0, new CommandLine(runner).execute(args));
    }

    @Test
    void testStoredProcedureExecution() throws Exception {
        // First create the procedure
        File procScript = createTestScript("""
            CREATE OR REPLACE PROCEDURE test_proc(
                p_id IN NUMBER,
                p_name OUT VARCHAR2
            ) AS
            BEGIN
                SELECT name INTO p_name FROM test_table WHERE id = p_id;
            END;
            /
            """);

        String[] createArgs = buildBaseArgs();
        createArgs[createArgs.length - 1] = procScript.getAbsolutePath();
        assertEquals(0, new CommandLine(runner).execute(createArgs));

        // Then execute the procedure
        String[] procArgs = buildBaseArgs();
        procArgs[procArgs.length - 1] = "test_proc";
        procArgs = appendArgs(procArgs, "-i", "p_id:NUMBER:1", "-o", "p_name:VARCHAR2");
        assertEquals(0, new CommandLine(runner).execute(procArgs));
    }

    @Test
    void testFunctionExecution() throws Exception {
        // First create the function
        File funcScript = createTestScript("""
            CREATE OR REPLACE FUNCTION test_func(p_id IN NUMBER)
            RETURN VARCHAR2 AS
                v_name VARCHAR2(100);
            BEGIN
                SELECT name INTO v_name FROM test_table WHERE id = p_id;
                RETURN v_name;
            END;
            /
            """);

        String[] createArgs = buildBaseArgs();
        createArgs[createArgs.length - 1] = funcScript.getAbsolutePath();
        assertEquals(0, new CommandLine(runner).execute(createArgs));

        // Then execute the function
        String[] funcArgs = buildBaseArgs();
        funcArgs[funcArgs.length - 1] = "test_func";
        funcArgs = appendArgs(funcArgs, "--function", "--return-type", "VARCHAR2", "-i", "p_id:NUMBER:1");
        assertEquals(0, new CommandLine(runner).execute(funcArgs));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAutoCommitOption(boolean autoCommit) throws Exception {
        File scriptFile = createTestScript("""
            INSERT INTO test_table VALUES (2, 'AutoCommit Test');
            """);

        String[] args = buildBaseArgs();
        args[args.length - 1] = scriptFile.getAbsolutePath();
        args = appendArgs(args, "--auto-commit", String.valueOf(autoCommit));

        assertEquals(0, new CommandLine(runner).execute(args));
    }

    @Test
    void testStopOnErrorOption() throws Exception {
        File scriptFile = createTestScript("""
            INSERT INTO non_existent_table VALUES (1, 'Test');
            INSERT INTO test_table VALUES (3, 'After Error');
            """);

        String[] args = buildBaseArgs();
        args[args.length - 1] = scriptFile.getAbsolutePath();
        args = appendArgs(args, "--stop-on-error", "true");

        assertNotEquals(0, new CommandLine(runner).execute(args));
    }

    @Test
    void testDynamicDriverLoading() throws Exception {
        File scriptFile = createTestScript("""
            SELECT 1 FROM DUAL;
            """);

        String driverPath = getClass().getClassLoader().getResource("ojdbc11.jar").getPath();
        String[] args = buildBaseArgs();
        args[args.length - 1] = scriptFile.getAbsolutePath();
        args = appendArgs(args, "--driver-path", driverPath);

        assertEquals(0, new CommandLine(runner).execute(args));
    }

    private String[] buildBaseArgs() {
        return new String[] {
            "-t", "oracle",
            "-H", oracle.getHost(),
            "-P", String.valueOf(oracle.getFirstMappedPort()),
            "-u", "HR",
            "-p", "hrpass",
            "-d", "XE",
            "--print-statements", "true",
            "" // Placeholder for script file or procedure name
        };
    }

    private String[] appendArgs(String[] baseArgs, String... newArgs) {
        String[] result = new String[baseArgs.length + newArgs.length];
        System.arraycopy(baseArgs, 0, result, 0, baseArgs.length);
        System.arraycopy(newArgs, 0, result, baseArgs.length, newArgs.length);
        return result;
    }

    private File createTestScript(String content) throws IOException {
        File scriptFile = tempDir.resolve("test_" + System.currentTimeMillis() + ".sql").toFile();
        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write(content);
        }
        return scriptFile;
    }
}