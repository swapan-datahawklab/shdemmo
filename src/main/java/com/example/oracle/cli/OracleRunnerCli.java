package com.example.oracle.cli;

import com.example.oracle.service.OracleScriptRunner;
import com.example.oracle.service.OracleStoredProcRunner;
import com.example.oracle.service.OracleStoredProcRunner.ProcedureParam;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "oracle-runner",
    mixinStandardHelpOptions = true,
    version = "1.0",
    description = "Executes Oracle SQL scripts and stored procedures"
)
public class OracleRunnerCli implements Callable<Integer> {
    @Option(names = {"-H", "--host"}, description = "Database host (e.g., localhost:1521/ORCL)", required = true)
    private String host;

    @Option(names = {"-u", "--username"}, description = "Database username", required = true)
    private String username;

    @Option(names = {"-p", "--password"}, description = "Database password", required = true)
    private String password;

    @Parameters(index = "0", description = "SQL type (script or proc)")
    private String sqlType;

    @Parameters(index = "1", description = "Script file path or procedure name", arity = "1")
    private String target;

    @Option(names = "--stop-on-error", description = "Stop execution on first error", defaultValue = "true")
    private boolean stopOnError;

    @Option(names = "--auto-commit", description = "Auto-commit after each statement", defaultValue = "false")
    private boolean autoCommit;

    @Option(names = "--print-statements", description = "Print executed statements", defaultValue = "false")
    private boolean printStatements;

    @Option(names = "--function", description = "Execute as function instead of procedure")
    private boolean isFunction;

    @Option(names = "--return-type", description = "Return type for functions (e.g., NUMERIC, VARCHAR)")
    private String returnType;

    @Option(names = {"-i", "--in"}, description = "Input parameters (name:type:value)")
    private String[] inParams;

    @Option(names = {"-o", "--out"}, description = "Output parameters (name:type)")
    private String[] outParams;

    @Option(names = "--io", description = "Input/Output parameters (name:type:value)")
    private String[] inOutParams;

    @Option(names = "--print-output", description = "Print output from procedure", defaultValue = "true")
    private boolean printOutput;

    private OracleScriptRunner scriptRunner;
    private OracleStoredProcRunner procRunner;

    public void setScriptRunner(OracleScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner;
    }

    public void setProcRunner(OracleStoredProcRunner procRunner) {
        this.procRunner = procRunner;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new OracleRunnerCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@" + host,
                username,
                password
        )) {
            switch (sqlType.toLowerCase()) {
                case "script":
                    return runScript(conn);
                case "proc":
                    return runProcedure(conn);
                default:
                    System.err.println("Invalid SQL type. Must be 'script' or 'proc'");
                    return 1;
            }
        }
    }

    private Integer runScript(Connection conn) throws Exception {
        if (scriptRunner == null) {
            scriptRunner = new OracleScriptRunner(conn);
        }

        scriptRunner.setStopOnError(stopOnError)
                      .setAutoCommit(autoCommit)
                      .setPrintStatements(printStatements);

        scriptRunner.runScript(new File(target));
        return 0;
    }

    private Integer runProcedure(Connection conn) throws SQLException {
        if (procRunner == null) {
            procRunner = new OracleStoredProcRunner(conn);
        }

        if (isFunction) {
            return runFunction(conn);
        } else {
            return runStoredProcedure(conn);
        }
    }

    private Integer runFunction(Connection conn) throws SQLException {
        if (returnType == null) {
            System.err.println("Return type must be specified for functions");
            return 1;
        }

        int sqlType = getSqlType(returnType);
        if (sqlType == -1) {
            System.err.println("Invalid return type: " + returnType);
            return 1;
        }

        List<ProcedureParam> params = new ArrayList<>();

        // Add input parameters
        if (inParams != null) {
            for (String param : inParams) {
                String[] parts = param.split(":");
                params.add(ProcedureParam.in(parts[0], sqlType, parseValue(parts[2])));
            }
        }

        // Add output parameters
        if (outParams != null) {
            for (String param : outParams) {
                String[] parts = param.split(":");
                params.add(ProcedureParam.out(parts[0], sqlType));
            }
        }

        // Add input/output parameters
        if (inOutParams != null) {
            for (String param : inOutParams) {
                String[] parts = param.split(":");
                params.add(ProcedureParam.inOut(parts[0], sqlType, parseValue(parts[2])));
            }
        }

        Object result = procRunner.executeFunction(target, sqlType, params.toArray(new ProcedureParam[0]));
        System.out.println("Function result: " + result);
        return 0;
    }

    private Integer runStoredProcedure(Connection conn) throws SQLException {
        List<ProcedureParam> params = new ArrayList<>();

        // Add input parameters
        if (inParams != null) {
            for (String param : inParams) {
                String[] parts = param.split(":");
                params.add(ProcedureParam.in(parts[0], getSqlType(parts[1]), parseValue(parts[2])));
            }
        }

        // Add output parameters
        if (outParams != null) {
            for (String param : outParams) {
                String[] parts = param.split(":");
                params.add(ProcedureParam.out(parts[0], getSqlType(parts[1])));
            }
        }

        // Add input/output parameters
        if (inOutParams != null) {
            for (String param : inOutParams) {
                String[] parts = param.split(":");
                params.add(ProcedureParam.inOut(parts[0], getSqlType(parts[1]), parseValue(parts[2])));
            }
        }

        procRunner.executeProcedure(target, params.toArray(new ProcedureParam[0]));
        return 0;
    }

    private int getSqlType(String type) {
        switch (type.toUpperCase()) {
            case "NUMERIC":
            case "NUMBER":
                return Types.NUMERIC;
            case "VARCHAR":
            case "VARCHAR2":
                return Types.VARCHAR;
            case "DATE":
                return Types.DATE;
            case "TIMESTAMP":
                return Types.TIMESTAMP;
            default:
                return -1;
        }
    }

    private Object parseValue(String value) {
        if (value == null || value.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }
} 