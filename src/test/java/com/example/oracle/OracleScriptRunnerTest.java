package com.example.oracle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OracleScriptRunnerTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @TempDir
    Path tempDir;

    private OracleScriptRunner runner;
    private File scriptFile;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        MockitoAnnotations.openMocks(this);

        // Set up mock behavior
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        // Create test script file
        scriptFile = tempDir.resolve("test.sql").toFile();
        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write("SELECT * FROM employees;\n");
            writer.write("UPDATE employees SET salary = salary * 1.1;\n");
            writer.write("BEGIN\n");
            writer.write("  UPDATE departments SET budget = budget + 1000;\n");
            writer.write("  COMMIT;\n");
            writer.write("END;\n");
        }

        runner = new OracleScriptRunner(mockConnection);
    }

    @Test
    void testRunScript() throws SQLException, IOException {
        // Configure runner
        runner.setStopOnError(true)
              .setAutoCommit(false)
              .setPrintStatements(true);

        // Run script
        runner.runScript(scriptFile);

        // Verify statements were executed
        verify(mockStatement).executeQuery("SELECT * FROM employees");
        verify(mockStatement).executeUpdate("UPDATE employees SET salary = salary * 1.1");
        verify(mockStatement).execute(
            "BEGIN\n  UPDATE departments SET budget = budget + 1000;\n  COMMIT;\nEND;"
        );
    }

    @Test
    void testRunScriptWithError() throws SQLException, IOException {
        // Simulate SQL error
        when(mockStatement.executeUpdate(anyString()))
            .thenThrow(new SQLException("ORA-00001: unique constraint violated", "ORA-00001", 1));

        // Configure runner
        runner.setStopOnError(true);

        // Run script and verify exception
        SQLException exception = assertThrows(SQLException.class, () -> runner.runScript(scriptFile));
        assertTrue(exception.getMessage().contains("ORA-00001"));
    }

    @Test
    void testPrintResultSet() throws SQLException, IOException {
        // Set up mock ResultSet
        when(mockResultSet.getMetaData()).thenReturn(mock(java.sql.ResultSetMetaData.class));
        when(mockResultSet.getMetaData().getColumnCount()).thenReturn(2);
        when(mockResultSet.getMetaData().getColumnName(1)).thenReturn("ID");
        when(mockResultSet.getMetaData().getColumnName(2)).thenReturn("NAME");
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString(1)).thenReturn("1", "2");
        when(mockResultSet.getString(2)).thenReturn("John", "Jane");

        // Execute query and print results
        runner.setPrintStatements(true);
        runner.runScript(scriptFile);

        // Verify ResultSet was processed
        verify(mockResultSet, times(3)).next();
        verify(mockResultSet, times(2)).getString(1);
        verify(mockResultSet, times(2)).getString(2);
    }

    @Test
    void testAutoCommitBehavior() throws SQLException, IOException {
        // Configure runner
        runner.setAutoCommit(false);

        // Run script
        runner.runScript(scriptFile);

        // Verify commit was called
        verify(mockConnection).commit();
    }

    @Test
    void testClose() throws SQLException, IOException {
        // Run and close
        runner.runScript(scriptFile);
        runner.close();

        // Verify connection was closed
        verify(mockConnection).close();
    }
} 