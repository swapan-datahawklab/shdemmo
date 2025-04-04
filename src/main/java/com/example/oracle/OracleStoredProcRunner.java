package com.example.oracle;

import java.sql.*;
import java.util.logging.Logger;

/**
 * A stored procedure runner for Oracle that provides SQLPlus-like functionality and error handling.
 * Supports executing stored procedures with proper error reporting and parameter handling.
 */
public class OracleStoredProcRunner implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(OracleStoredProcRunner.class.getName());
    private final Connection connection;
    private boolean printOutput = true;
    
    public enum ParameterType {
        IN, OUT, INOUT
    }

    /**
     * Creates a new OracleStoredProcRunner with the specified connection.
     * @param connection JDBC connection to Oracle database
     */
    public OracleStoredProcRunner(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        this.connection = connection;
    }

    /**
     * Sets whether to print procedure output.
     * @param printOutput true to print output, false to suppress
     * @return this instance for method chaining
     */
    public OracleStoredProcRunner setPrintOutput(boolean printOutput) {
        this.printOutput = printOutput;
        return this;
    }

    /**
     * Executes a stored procedure with the given name and parameters.
     * @param procedureName the name of the stored procedure
     * @param params the parameters to pass to the procedure
     * @throws SQLException if there's an error executing the procedure
     */
    public void executeProcedure(String procedureName, ProcedureParam... params) throws SQLException {
        String callString = buildCallString(procedureName, params);
        
        try (CallableStatement stmt = connection.prepareCall(callString)) {
            setParameters(stmt, params);
            stmt.execute();
            getOutParameters(stmt, params);
        } catch (SQLException e) {
            String errorMessage = formatOracleError(e);
            LOGGER.severe(errorMessage);
            throw e;
        }
    }

    /**
     * Executes a stored function with the given name and parameters.
     * @param functionName the name of the stored function
     * @param returnType the SQL type of the return value
     * @param params the parameters to pass to the function
     * @return the function's return value
     * @throws SQLException if there's an error executing the function
     */
    public Object executeFunction(String functionName, int returnType, ProcedureParam... params) throws SQLException {
        String callString = buildFunctionCallString(functionName, params);
        
        try (CallableStatement stmt = connection.prepareCall(callString)) {
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

    /**
     * Builds the call string for a stored procedure.
     */
    private String buildCallString(String procedureName, ProcedureParam[] params) {
        StringBuilder sb = new StringBuilder();
        sb.append("{call ").append(procedureName).append("(");
        
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        
        sb.append(")}");
        return sb.toString();
    }

    /**
     * Builds the call string for a stored function.
     */
    private String buildFunctionCallString(String functionName, ProcedureParam[] params) {
        StringBuilder sb = new StringBuilder();
        sb.append("{? = call ").append(functionName).append("(");
        
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("?");
        }
        
        sb.append(")}");
        return sb.toString();
    }

    private void setParameters(CallableStatement stmt, ProcedureParam[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ProcedureParam param = params[i];
            int paramIndex = i + 1;

            if (param.getType() != ParameterType.OUT) {
                setParameter(stmt, paramIndex, param.getSqlType(), param.getValue());
            }
            if (param.getType() != ParameterType.IN) {
                stmt.registerOutParameter(paramIndex, param.getSqlType());
            }
        }
    }

    private void setFunctionParameters(CallableStatement stmt, ProcedureParam[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ProcedureParam param = params[i];
            int paramIndex = i + 2; // Add 2 because index 1 is reserved for return value

            if (param.getType() != ParameterType.OUT) {
                setParameter(stmt, paramIndex, param.getSqlType(), param.getValue());
            }
            if (param.getType() != ParameterType.IN) {
                stmt.registerOutParameter(paramIndex, param.getSqlType());
            }
        }
    }

    private void setParameter(CallableStatement stmt, int index, int sqlType, Object value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, sqlType);
            return;
        }

        switch (sqlType) {
            case Types.INTEGER:
            case Types.NUMERIC:
                if (value instanceof Integer) {
                    stmt.setInt(index, (Integer) value);
                } else if (value instanceof java.math.BigDecimal) {
                    stmt.setBigDecimal(index, (java.math.BigDecimal) value);
                } else {
                    stmt.setObject(index, value, sqlType);
                }
                break;
            case Types.VARCHAR:
                stmt.setString(index, value.toString());
                break;
            case Types.DATE:
                if (value instanceof java.sql.Date) {
                    stmt.setDate(index, (java.sql.Date) value);
                } else if (value instanceof java.util.Date) {
                    stmt.setDate(index, new java.sql.Date(((java.util.Date) value).getTime()));
                }
                break;
            case Types.TIMESTAMP:
                if (value instanceof java.sql.Timestamp) {
                    stmt.setTimestamp(index, (java.sql.Timestamp) value);
                } else if (value instanceof java.util.Date) {
                    stmt.setTimestamp(index, new java.sql.Timestamp(((java.util.Date) value).getTime()));
                }
                break;
            default:
                stmt.setObject(index, value, sqlType);
        }
    }

    private void getOutParameters(CallableStatement stmt, ProcedureParam[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ProcedureParam param = params[i];
            if (param.getType() != ParameterType.IN) {
                int paramIndex = i + 1;
                Object value = stmt.getObject(paramIndex);
                if (printOutput) {
                    LOGGER.info(String.format("OUT parameter %s = %s", param.getName(), value));
                }
            }
        }
    }

    /**
     * Formats Oracle errors in a SQLPlus-like format.
     */
    private String formatOracleError(SQLException e) {
        StringBuilder error = new StringBuilder();
        error.append("ERROR: ");
        
        if (e.getErrorCode() > 0) {
            error.append("ORA-").append(String.format("%05d", e.getErrorCode()))
                 .append(": ");
        }
        
        error.append(e.getMessage());
        
        // Handle chained exceptions
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

    /**
     * Parameter class for stored procedures and functions.
     */
    public static class ProcedureParam {
        private final String name;
        private final int sqlType;
        private final Object value;
        private final ParameterType type;

        public ProcedureParam(String name, int sqlType, Object value, ParameterType type) {
            this.name = name;
            this.sqlType = sqlType;
            this.value = value;
            this.type = type;
        }

        public static ProcedureParam in(String name, int sqlType, Object value) {
            return new ProcedureParam(name, sqlType, value, ParameterType.IN);
        }

        public static ProcedureParam out(String name, int sqlType) {
            return new ProcedureParam(name, sqlType, null, ParameterType.OUT);
        }

        public static ProcedureParam inOut(String name, int sqlType, Object value) {
            return new ProcedureParam(name, sqlType, value, ParameterType.INOUT);
        }

        public String getName() { return name; }
        public int getSqlType() { return sqlType; }
        public Object getValue() { return value; }
        public ParameterType getType() { return type; }
    }
} 