package com.example.oracle.cli;

import com.example.oracle.OracleScriptRunner;
import com.example.oracle.OracleStoredProcRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OracleRunnerCliTest {
    
    @Mock
    private Connection mockConnection;
    
    @TempDir
    Path tempDir;
    
    private File scriptFile;
    
    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        
        // Create a temporary SQL script file
        scriptFile = tempDir.resolve("test.sql").toFile();
        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write("SELECT * FROM test_table;\n");
            writer.write("UPDATE test_table SET col1 = 'value';\n");
        }
    }
    
    @Test
    void testRunScript() throws SQLException, IOException {
        // Arrange
        String[] args = {
            "script",
            "-H", "localhost:1521/ORCL",
            "-u", "test_user",
            "-p", "test_pass",
            scriptFile.getAbsolutePath(),
            "--print-statements"
        };
        
        // Create mock script runner
        OracleScriptRunner mockRunner = mock(OracleScriptRunner.class);
        when(mockRunner.setStopOnError(anyBoolean())).thenReturn(mockRunner);
        when(mockRunner.setAutoCommit(anyBoolean())).thenReturn(mockRunner);
        when(mockRunner.setPrintStatements(anyBoolean())).thenReturn(mockRunner);
        
        // Execute command
        int exitCode = new CommandLine(new OracleRunnerCli()).execute(args);
        
        // Assert
        assertEquals(0, exitCode);
        verify(mockRunner, times(1)).runScript(eq(scriptFile));
    }
    
    @Test
    void testRunProcedure() throws SQLException, IOException {
        // Arrange
        String[] args = {
            "proc",
            "-H", "localhost:1521/ORCL",
            "-u", "test_user",
            "-p", "test_pass",
            "UPDATE_EMPLOYEE_SALARY",
            "-i", "p_emp_id:NUMERIC:101,p_percentage:NUMERIC:10.5",
            "-o", "p_new_salary:NUMERIC"
        };
        
        // Create mock stored proc runner
        OracleStoredProcRunner mockRunner = mock(OracleStoredProcRunner.class);
        
        // Execute command
        int exitCode = new CommandLine(new OracleRunnerCli()).execute(args);
        
        // Assert
        assertEquals(0, exitCode);
        verify(mockRunner, times(1)).executeProcedure(
            eq("UPDATE_EMPLOYEE_SALARY"),
            any(OracleStoredProcRunner.ProcedureParam[].class)
        );
    }
    
    @Test
    void testRunFunction() throws SQLException, IOException {
        // Arrange
        String[] args = {
            "proc",
            "-H", "localhost:1521/ORCL",
            "-u", "test_user",
            "-p", "test_pass",
            "GET_DEPARTMENT_BUDGET",
            "--function",
            "--return-type", "NUMERIC",
            "-i", "p_dept_id:NUMERIC:20"
        };
        
        // Create mock stored proc runner
        OracleStoredProcRunner mockRunner = mock(OracleStoredProcRunner.class);
        when(mockRunner.executeFunction(
            anyString(),
            anyInt(),
            any(OracleStoredProcRunner.ProcedureParam[].class)
        )).thenReturn(1000.0);
        
        // Execute command
        int exitCode = new CommandLine(new OracleRunnerCli()).execute(args);
        
        // Assert
        assertEquals(0, exitCode);
        verify(mockRunner, times(1)).executeFunction(
            eq("GET_DEPARTMENT_BUDGET"),
            eq(Types.NUMERIC),
            any(OracleStoredProcRunner.ProcedureParam[].class)
        );
    }
    
    @Test
    void testInvalidSqlType() throws IOException {
        // Arrange
        String[] args = {
            "proc",
            "-H", "localhost:1521/ORCL",
            "-u", "test_user",
            "-p", "test_pass",
            "TEST_PROC",
            "-i", "param1:INVALID_TYPE:value"
        };
        
        // Execute command
        int exitCode = new CommandLine(new OracleRunnerCli()).execute(args);
        
        // Assert
        assertEquals(1, exitCode);
    }
} 