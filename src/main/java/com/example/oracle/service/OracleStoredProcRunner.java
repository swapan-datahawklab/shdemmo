package com.example.oracle.service;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Executes Oracle stored procedures and functions with parameter handling.
 */
public class OracleStoredProcRunner implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(OracleStoredProcRunner.class.getName());
    private final Connection connection;
    private final boolean printOutput;

    public enum ParameterType {
        IN, OUT, INOUT
    }

    /**
     * Creates a new OracleStoredProcRunner with the specified connection.
     * @param connection JDBC connection to Oracle database
     * @param printOutput whether to print output parameter values
     */
    public OracleStoredProcRunner(Connection connection, boolean printOutput) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
        this.printOutput = printOutput;
    }

    /**
     * Represents a parameter for a stored procedure or function.
     */
    public static class ProcedureParam {
        private final String name;
        private final int sqlType;
        private final Object value;
        private final ParameterType type;

        private ProcedureParam(String name, int sqlType, Object value, ParameterType type) {
            this.name = name;
            this.sqlType = sqlType;
            this.value = value;
            this.type = type;
        }

        public String getName() { return name; }
        public int getSqlType() { return sqlType; }
        public Object getValue() { return value; }
        public ParameterType getType() { return type; }

        public static ProcedureParam in(String name, int sqlType, Object value) {
            return new ProcedureParam(name, sqlType, value, ParameterType.IN);
        }

        public static ProcedureParam out(String name, int sqlType) {
            return new ProcedureParam(name, sqlType, null, ParameterType.OUT);
        }

        public static ProcedureParam inOut(String name, int sqlType, Object value) {
            return new ProcedureParam(name, sqlType, value, ParameterType.INOUT);
        }
    }

    /**
     * Executes a stored procedure with the specified parameters.
     * @param procedureName name of the stored procedure
     * @param params array of procedure parameters
     * @return map of output parameter names to their values
     * @throws SQLException if there's an error executing the procedure
     */
    public Map<String, Object> executeProcedure(String procedureName, ProcedureParam... params) throws SQLException {
        String sql = buildProcedureCall(procedureName, params);
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            setParameters(stmt, params);
            stmt.execute();
            return getOutParameters(stmt, params);
        } catch (SQLException e) {
            String errorMessage = formatOracleError(e);
            LOGGER.severe(errorMessage);
            throw e;
        }
    }

    /**
     * Executes a stored function with the specified parameters and return type.
     * @param functionName name of the stored function
     * @param returnType SQL type of the return value
     * @param params array of function parameters
     * @return the function's return value
     * @throws SQLException if there's an error executing the function
     */
    public Object executeFunction(String functionName, int returnType, ProcedureParam... params) throws SQLException {
        String sql = buildFunctionCall(functionName, params);
        try (CallableStatement stmt = connection.prepareCall(sql)) {
            stmt.registerOutParameter(1, returnType);
            setFunctionParameters(stmt, params);
            stmt.execute();
            return stmt.getObject(1);
        } catch (SQLException e) {
            String errorMessage = formatOracleError(e);
            LOGGER.severe(errorMessage);
            throw e;
        }
    }

    private String buildProcedureCall(String procedureName, ProcedureParam[] params) {
        StringBuilder sql = new StringBuilder();
        sql.append("{call ").append(procedureName).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")}");
        return sql.toString();
    }

    private String buildFunctionCall(String functionName, ProcedureParam[] params) {
        StringBuilder sql = new StringBuilder();
        sql.append("{? = call ").append(functionName).append("(");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")}");
        return sql.toString();
    }

    private void setParameters(CallableStatement stmt, ProcedureParam[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ProcedureParam param = params[i];
            int paramIndex = i + 1;

            if (param.type == ParameterType.IN) {
                stmt.setObject(paramIndex, param.value);
            } else {
                stmt.registerOutParameter(paramIndex, param.sqlType);
                if (param.type == ParameterType.INOUT) {
                    stmt.setObject(paramIndex, param.value);
                }
            }
        }
    }

    private void setFunctionParameters(CallableStatement stmt, ProcedureParam[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ProcedureParam param = params[i];
            int paramIndex = i + 2; // Add 2 because index 1 is reserved for return value

            if (param.type == ParameterType.IN) {
                stmt.setObject(paramIndex, param.value);
            } else {
                stmt.registerOutParameter(paramIndex, param.sqlType);
                if (param.type == ParameterType.INOUT) {
                    stmt.setObject(paramIndex, param.value);
                }
            }
        }
    }

    private Map<String, Object> getOutParameters(CallableStatement stmt, ProcedureParam[] params) throws SQLException {
        Map<String, Object> outValues = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            ProcedureParam param = params[i];
            if (param.type == ParameterType.OUT || param.type == ParameterType.INOUT) {
                int paramIndex = i + 1;
                Object value = stmt.getObject(paramIndex);
                outValues.put(param.name, value);
                if (printOutput) {
                    LOGGER.info(String.format("OUT parameter %s = %s", param.name, value));
                }
            }
        }
        return outValues;
    }

    private String formatOracleError(SQLException e) {
        StringBuilder error = new StringBuilder();
        error.append("ORA-").append(String.format("%05d", e.getErrorCode()))
             .append(": ").append(e.getMessage());
        
        SQLException nextException = e.getNextException();
        while (nextException != null) {
            error.append("\nCaused by: ORA-")
                 .append(String.format("%05d", nextException.getErrorCode()))
                 .append(": ")
                 .append(nextException.getMessage());
            nextException = nextException.getNextException();
        }
        
        return error.toString();
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
} 