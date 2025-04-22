package com.example.shelldemo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.shelldemo.parser.StoredProcedureParser;
import com.example.shelldemo.parser.StoredProcedureParser.ProcedureParam;
import com.example.shelldemo.connection.ConnectionConfig;


import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * A unified database runner that combines CLI and script execution functionality.
 * Supports executing SQL scripts and stored procedures with proper error handling.
 */
@Command(name = "db", mixinStandardHelpOptions = true, version = "1.0",
    description = "Unified Database CLI Tool")
public class UnifiedDatabaseRunner implements Callable<Integer> {
    private final Logger logger;
    private final Logger methodLogger;
    private UnifiedDatabaseOperation dbOperation;
    
    // Add a factory interface
    @FunctionalInterface
    public interface DatabaseOperationFactory {
        UnifiedDatabaseOperation create(String dbType, ConnectionConfig config) throws IOException;
    }
    
    private final DatabaseOperationFactory operationFactory;
    
    // Default constructor for CLI use
    public UnifiedDatabaseRunner() {
        this(
            LoggerFactory.getLogger(UnifiedDatabaseRunner.class),
            LoggerFactory.getLogger(UnifiedDatabaseRunner.class.getName() + ".methods"),
            UnifiedDatabaseOperation::create
        );
    }
    
    // Test constructor
    public UnifiedDatabaseRunner(Logger logger, Logger methodLogger, DatabaseOperationFactory factory) {
        this.logger = logger;
        this.methodLogger = methodLogger;
        this.operationFactory = factory;
    }

    @Option(names = {"-t", "--type"}, required = true,
        description = "Database type (oracle, sqlserver, postgresql, mysql)")
    private String dbType;

    @Option(names = {"--connection-type"}, description = "Connection type for Oracle (thin, ldap). Defaults to ldap if not specified.")
    private String connectionType;

    @Option(names = {"-H", "--host"}, required = true, description = "Database host")
    private String host;

    @Option(names = {"-P", "--port"},
        description = "Database port (defaults: oracle=1521, sqlserver=1433, postgresql=5432, mysql=3306)")
    private int port;

    @Option(names = {"-u", "--username"}, required = true, description = "Database username")
    private String username;

    @Option(names = {"-p", "--password"}, required = true, description = "Database password")
    private String password;

    @Option(names = {"-d", "--database"}, required = true, description = "Database name")
    private String database;

    @Option(names = {"--stop-on-error"}, defaultValue = "true",
        description = "Stop execution on error")
    private boolean stopOnError;

    @Option(names = {"--auto-commit"}, defaultValue = "false",
        description = "Auto-commit mode")
    private boolean autoCommit;

    @Option(names = {"--print-statements"}, defaultValue = "false",
        description = "Print SQL statements")
    private boolean printStatements;

    @Parameters(index = "0", description = "SQL script file or stored procedure name")
    private String target;

    @Option(names = {"--function"}, description = "Execute as function")
    private boolean isFunction;

    @Option(names = {"--return-type"}, defaultValue = "NUMERIC",
        description = "Return type for functions")
    private String returnType;

    @Option(names = {"-i", "--input"}, description = "Input parameters (name:type:value,...)")
    private String inputParams;

    @Option(names = {"-o", "--output"}, description = "Output parameters (name:type,...)")
    private String outputParams;

    @Option(names = {"--io"}, description = "Input/Output parameters (name:type:value,...)")
    private String ioParams;

    @Option(names = {"--driver-path"}, description = "Path to JDBC driver JAR file")
    private String driverPath;

    @Option(names = {"--csv-output"}, description = "Output file for CSV format (if query results exist)")
    private String csvOutputFile;

    private void initializeConfig() throws IOException, SQLException {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(host);
        config.setUsername(username);
        config.setPassword(password);
        config.setServiceName(database);
        config.setPort(port);
        config.setDbType(dbType);
        config.setConnectionType(connectionType);

        this.dbOperation = operationFactory.create(dbType, config);
    }

    private List<ProcedureParam> parseParameters() {
        methodLogger.debug("[parseParameters] Starting parameter parsing");
        methodLogger.trace("[parseParameters] Raw inputs - IN: {}, OUT: {}, INOUT: {}", 
            inputParams, outputParams, ioParams);
            
        List<ProcedureParam> params = StoredProcedureParser.parse(inputParams, outputParams, ioParams);
        methodLogger.debug("[parseParameters] Successfully parsed {} parameters", params.size());
        methodLogger.trace("[parseParameters] Parsed parameters: {}", params);
        return params;
    }

    @Override
    public Integer call() throws Exception {
        logger.info("Starting database operation - type: {}, target: {}", dbType, target);
        methodLogger.debug("[call] Beginning execution with type: {}, target: {}", dbType, target);
        
        if (driverPath != null) {
            logger.info("Loading custom JDBC driver from: {}", driverPath);
            dbOperation.loadDriverFromPath(driverPath);
        }

        try {
            initializeConfig();
            File scriptFile = new File(target);

            if (scriptFile.exists()) {
                methodLogger.debug("[call] Executing as script file: {}", scriptFile.getAbsolutePath());
                dbOperation.executeScript(scriptFile, printStatements);
                return 0;
            } else {
                methodLogger.debug("[call] Executing as stored procedure: {}", target);
                dbOperation.executeStoredProcedure(target, isFunction, parseParameters().toArray());
                return 0;
            }
        } catch (Exception e) {
            logger.error("Operation failed: {}", e.getMessage(), e);
            methodLogger.error("[call] Execution failed with error: {}", e.getMessage(), e);
            throw e;
        }
    }

    public static void main(String[] args) {
        // Enable Log4j2 async logging by default
        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize", "262144");
        System.setProperty("AsyncLogger.WaitStrategy", "Yield");
        
        // Create a static logger for main method
        Logger mainLogger = LoggerFactory.getLogger(UnifiedDatabaseRunner.class);
        
        mainLogger.info("Starting UnifiedDatabaseRunner...");
        int exitCode = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        mainLogger.info("UnifiedDatabaseRunner completed with exit code: {}", exitCode);
        System.exit(exitCode);
    }
}