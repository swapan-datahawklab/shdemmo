package com.example.oracle.service;

import com.example.oracle.datasource.DatabaseOperation;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * A stored procedure runner for Oracle that provides SQLPlus-like functionality and error handling.
 * Supports executing stored procedures with proper error reporting and parameter handling.
 */
public class OracleStoredProcRunner extends DatabaseOperation {
    private final boolean isFunction;
    private final List<ProcedureParam> inputParams;
    private final List<ProcedureParam> outputParams;
    private final List<ProcedureParam> inOutParams;
    private final boolean printOutput;

    public OracleStoredProcRunner(String host, String username, String password,
                                boolean isFunction,
                                List<ProcedureParam> inputParams,
                                List<ProcedureParam> outputParams,
                                List<ProcedureParam> inOutParams,
                                boolean printOutput) {
        super(host, username, password);
        this.isFunction = isFunction;
        this.inputParams = inputParams;
        this.outputParams = outputParams;
        this.inOutParams = inOutParams;
        this.printOutput = printOutput;
    }

    public Object execute(String procedureName) throws Exception {
        return execute(connection -> {
            try {
                String call = buildCallString(procedureName);
                try (CallableStatement stmt = connection.prepareCall(call)) {
                    // Register parameters
                    int paramIndex = 1;
                    
                    // Register input parameters
                    for (ProcedureParam param : inputParams) {
                        stmt.setObject(paramIndex++, param.getValue());
                    }
                    
                    // Register output parameters
                    for (ProcedureParam param : outputParams) {
                        stmt.registerOutParameter(paramIndex++, getSqlType(param.getType()));
                    }
                    
                    // Register in/out parameters
                    for (ProcedureParam param : inOutParams) {
                        stmt.setObject(paramIndex, param.getValue());
                        stmt.registerOutParameter(paramIndex++, getSqlType(param.getType()));
                    }
                    
                    // Execute the procedure/function
                    if (isFunction) {
                        stmt.execute();
                        Object result = stmt.getObject(1);
                        if (printOutput) {
                            System.out.println("Function result: " + result);
                        }
                        return result;
                    } else {
                        stmt.execute();
                        if (printOutput) {
                            printOutputParams(stmt);
                        }
                        return null;
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error executing " + (isFunction ? "function" : "procedure") + " " + procedureName + ": " + e.getMessage());
                throw e;
            } catch (Exception e) {
                System.err.println("Error executing " + (isFunction ? "function" : "procedure") + " " + procedureName + ": " + e.getMessage());
                throw new RuntimeException("Failed to execute " + (isFunction ? "function" : "procedure") + " " + procedureName, e);
            }
        });
    }

    private String buildCallString(String procedureName) {
        StringBuilder call = new StringBuilder();
        int totalParams = inputParams.size() + outputParams.size() + inOutParams.size();
        
        if (isFunction) {
            call.append("{? = call ").append(procedureName).append("(");
        } else {
            call.append("{call ").append(procedureName).append("(");
        }
        
        for (int i = 0; i < totalParams; i++) {
            if (i > 0) call.append(", ");
            call.append("?");
        }
        
        call.append(")}");
        return call.toString();
    }

    private int getSqlType(String type) {
        switch (type.toUpperCase()) {
            case "NUMERIC":
            case "NUMBER":
                return Types.NUMERIC;
            case "VARCHAR":
            case "VARCHAR2":
            case "CHAR":
                return Types.VARCHAR;
            case "DATE":
                return Types.DATE;
            case "TIMESTAMP":
                return Types.TIMESTAMP;
            case "CLOB":
                return Types.CLOB;
            case "BLOB":
                return Types.BLOB;
            default:
                return Types.VARCHAR;
        }
    }

    private void printOutputParams(CallableStatement stmt) throws Exception {
        int paramIndex = inputParams.size() + 1;
        
        for (ProcedureParam param : outputParams) {
            Object value = stmt.getObject(paramIndex++);
            System.out.println(param.getName() + " = " + value);
        }
        
        for (ProcedureParam param : inOutParams) {
            Object value = stmt.getObject(paramIndex++);
            System.out.println(param.getName() + " = " + value);
        }
    }

    /**
     * Parameter class for stored procedures and functions.
     */
    public static class ProcedureParam {
        private final String name;
        private final String type;
        private final Object value;

        public ProcedureParam(String name, String type, Object value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }
} 