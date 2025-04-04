package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import com.example.oracle.cli.OracleRunnerCli;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppTest {
    @TempDir
    Path tempDir;

    @Test
    void testRunSqlScript() throws Exception {
        // Create test script file
        File scriptFile = tempDir.resolve("test.sql").toFile();
        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write("SELECT * FROM test_table;");
        }

        // Mock OracleRunnerCli.main
        try (MockedStatic<OracleRunnerCli> mockedCli = mockStatic(OracleRunnerCli.class)) {
            String[] args = new String[] {
                "script",
                "-H", "localhost:1521/ORCL",
                "-u", "test_user",
                "-p", "test_pass",
                "--stop-on-error=true",
                "--auto-commit=false",
                "--print-statements=true",
                scriptFile.getAbsolutePath()
            };
            App.main(args);

            mockedCli.verify(() -> OracleRunnerCli.main(any(String[].class)));
        }
    }

    @Test
    void testRunStoredProc() {
        try (MockedStatic<OracleRunnerCli> mockedCli = mockStatic(OracleRunnerCli.class)) {
            String[] args = new String[] {
                "proc",
                "-H", "localhost:1521/ORCL",
                "-u", "test_user",
                "-p", "test_pass",
                "TEST_PROC",
                "-i", "param1:NUMERIC:100",
                "-o", "out1:NUMERIC"
            };
            App.main(args);

            mockedCli.verify(() -> OracleRunnerCli.main(any(String[].class)));
        }
    }

    @Test
    void testRunFunction() {
        try (MockedStatic<OracleRunnerCli> mockedCli = mockStatic(OracleRunnerCli.class)) {
            String[] args = new String[] {
                "proc",
                "-H", "localhost:1521/ORCL",
                "-u", "test_user",
                "-p", "test_pass",
                "TEST_FUNC",
                "--function",
                "--return-type", "NUMERIC",
                "-i", "param1:NUMERIC:100"
            };
            App.main(args);

            mockedCli.verify(() -> OracleRunnerCli.main(any(String[].class)));
        }
    }

    @Test
    void testInvalidCommand() {
        try (MockedStatic<OracleRunnerCli> mockedCli = mockStatic(OracleRunnerCli.class)) {
            String[] args = new String[] {"invalid"};
            RuntimeException e = assertThrows(RuntimeException.class, () -> App.run(args));
            assertEquals("Unknown command: invalid", e.getMessage());
            mockedCli.verifyNoInteractions();
        }
    }
} 