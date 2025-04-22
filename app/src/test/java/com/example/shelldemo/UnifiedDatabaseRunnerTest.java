package com.example.shelldemo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
public class UnifiedDatabaseRunnerTest {
    
    @Mock
    private Logger logger;
    
    @Mock
    private Logger methodLogger;
    
    @Mock
    private UnifiedDatabaseOperation dbOperation;
    
    private UnifiedDatabaseRunner runner;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create the runner with a factory that returns our mock
        UnifiedDatabaseRunner.DatabaseOperationFactory factory = (dbType, config) -> dbOperation;
        runner = new UnifiedDatabaseRunner(logger, methodLogger, factory);
        
        // Set required fields
        setRequiredFields(runner);
        
        // Initialize the dbOperation field
        Field dbOperationField = UnifiedDatabaseRunner.class.getDeclaredField("dbOperation");
        dbOperationField.setAccessible(true);
        dbOperationField.set(runner, dbOperation);
    }
    
    private void setRequiredFields(UnifiedDatabaseRunner runner) {
        try {
            var dbTypeField = UnifiedDatabaseRunner.class.getDeclaredField("dbType");
            var hostField = UnifiedDatabaseRunner.class.getDeclaredField("host");
            var usernameField = UnifiedDatabaseRunner.class.getDeclaredField("username");
            var passwordField = UnifiedDatabaseRunner.class.getDeclaredField("password");
            var databaseField = UnifiedDatabaseRunner.class.getDeclaredField("database");
            var targetField = UnifiedDatabaseRunner.class.getDeclaredField("target");
            
            dbTypeField.setAccessible(true);
            hostField.setAccessible(true);
            usernameField.setAccessible(true);
            passwordField.setAccessible(true);
            databaseField.setAccessible(true);
            targetField.setAccessible(true);
            
            dbTypeField.set(runner, "oracle");
            hostField.set(runner, "localhost");
            usernameField.set(runner, "test");
            passwordField.set(runner, "test");
            databaseField.set(runner, "testdb");
            targetField.set(runner, "test.sql");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set required fields", e);
        }
    }
    
    @Test
    @DisplayName("Test execute script file")
    public void testExecuteScriptFile() throws Exception {
        // Arrange
        var targetField = UnifiedDatabaseRunner.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(runner, "test.sql");
        
        // Create a spy on a real File object
        File fileSpy = spy(new File("test.sql"));
        when(fileSpy.exists()).thenReturn(true);
        
        // Mock the File constructor using MockedStatic
        try (MockedStatic<File> fileMock = mockStatic(File.class)) {
            fileMock.when(() -> new File("test.sql")).thenReturn(fileSpy);
            
            // Act
            int result = runner.call();
            
            // Assert
            assertEquals(0, result);
            verify(dbOperation).executeScript(same(fileSpy), eq(false));
        }
    }
    
    @Test
    @DisplayName("Test execute stored procedure")
    public void testExecuteStoredProcedure() throws Exception {
        // Arrange
        var targetField = UnifiedDatabaseRunner.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(runner, "test_proc");
        
        // Act
        int result = runner.call();
        
        // Assert
        assertEquals(0, result);
        verify(dbOperation).executeStoredProcedure(eq("test_proc"), eq(false), eq(new Object[0]));
    }
    
    @Test
    @DisplayName("Test driver path loading")
    public void testDriverPathLoading() throws Exception {
        // Arrange
        var driverPathField = UnifiedDatabaseRunner.class.getDeclaredField("driverPath");
        driverPathField.setAccessible(true);
        driverPathField.set(runner, "/path/to/driver.jar");
        
        var targetField = UnifiedDatabaseRunner.class.getDeclaredField("target");
        targetField.setAccessible(true);
        targetField.set(runner, "test.sql");
        
        // Act
        runner.call();
        
        // Assert
        verify(dbOperation).loadDriverFromPath("/path/to/driver.jar");
    }
}
