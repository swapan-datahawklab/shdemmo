package com.example.oracle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OracleStoredProcRunnerTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private CallableStatement mockCallableStatement;

    private OracleStoredProcRunner runner;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        
        // Set up mock behavior
        when(mockConnection.prepareCall(anyString())).thenReturn(mockCallableStatement);
        
        runner = new OracleStoredProcRunner(mockConnection);
    }

    @Test
    void testExecuteProcedure() throws SQLException {
        // Arrange
        String procName = "UPDATE_EMPLOYEE_SALARY";
        OracleStoredProcRunner.ProcedureParam[] params = {
            new OracleStoredProcRunner.ProcedureParam("p_emp_id", Types.NUMERIC, 101, OracleStoredProcRunner.ParameterType.IN),
            new OracleStoredProcRunner.ProcedureParam("p_percentage", Types.NUMERIC, new BigDecimal("10.5"), OracleStoredProcRunner.ParameterType.IN),
            new OracleStoredProcRunner.ProcedureParam("p_new_salary", Types.NUMERIC, null, OracleStoredProcRunner.ParameterType.OUT)
        };

        // Mock OUT parameter
        when(mockCallableStatement.getBigDecimal(3)).thenReturn(new BigDecimal("55000.00"));

        // Act
        runner.executeProcedure(procName, params);

        // Assert
        verify(mockCallableStatement).setInt(1, 101);
        verify(mockCallableStatement).setBigDecimal(2, new BigDecimal("10.5"));
        verify(mockCallableStatement).registerOutParameter(3, Types.NUMERIC);
        verify(mockCallableStatement).execute();
        verify(mockCallableStatement).getBigDecimal(3);
    }

    @Test
    void testExecuteFunction() throws SQLException {
        // Arrange
        String funcName = "GET_DEPARTMENT_BUDGET";
        OracleStoredProcRunner.ProcedureParam[] params = {
            new OracleStoredProcRunner.ProcedureParam("p_dept_id", Types.NUMERIC, 20, OracleStoredProcRunner.ParameterType.IN)
        };

        // Mock function return value
        when(mockCallableStatement.getBigDecimal(1)).thenReturn(new BigDecimal("100000.00"));

        // Act
        Object result = runner.executeFunction(funcName, Types.NUMERIC, params);

        // Assert
        verify(mockCallableStatement).registerOutParameter(1, Types.NUMERIC);
        verify(mockCallableStatement).setInt(2, 20);
        verify(mockCallableStatement).execute();
        assertEquals(new BigDecimal("100000.00"), result);
    }

    @Test
    void testExecuteProcedureWithInOutParam() throws SQLException {
        // Arrange
        String procName = "CALCULATE_BONUS";
        OracleStoredProcRunner.ProcedureParam[] params = {
            new OracleStoredProcRunner.ProcedureParam("p_salary", Types.NUMERIC, new BigDecimal("50000.00"), OracleStoredProcRunner.ParameterType.INOUT)
        };

        // Mock INOUT parameter
        when(mockCallableStatement.getBigDecimal(1)).thenReturn(new BigDecimal("55000.00"));

        // Act
        runner.executeProcedure(procName, params);

        // Assert
        verify(mockCallableStatement).setBigDecimal(1, new BigDecimal("50000.00"));
        verify(mockCallableStatement).registerOutParameter(1, Types.NUMERIC);
        verify(mockCallableStatement).execute();
        verify(mockCallableStatement).getBigDecimal(1);
    }

    @Test
    void testExecuteProcedureWithError() throws SQLException {
        // Arrange
        String procName = "INVALID_PROC";
        OracleStoredProcRunner.ProcedureParam[] params = {
            new OracleStoredProcRunner.ProcedureParam("p_param", Types.VARCHAR, "test", OracleStoredProcRunner.ParameterType.IN)
        };

        // Mock SQL error
        when(mockCallableStatement.execute())
            .thenThrow(new SQLException("ORA-06550: line 1, column 7: PLS-00201: identifier 'INVALID_PROC' must be declared", "ORA-06550", 6550));

        // Act & Assert
        SQLException exception = assertThrows(SQLException.class, () -> runner.executeProcedure(procName, params));
        assertTrue(exception.getMessage().contains("ORA-06550"));
        assertEquals("ORA-06550", exception.getSQLState());
        assertEquals(6550, exception.getErrorCode());
    }

    @Test
    void testClose() throws SQLException {
        // Act
        runner.close();

        // Assert
        verify(mockConnection).close();
    }
} 