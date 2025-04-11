package com.example.shelldemo;

import java.io.File;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.List;
import java.util.concurrent.Callable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.shelldemo.config.CustomDriver;
import com.example.shelldemo.model.entity.config.ConnectionConfig;
import com.example.shelldemo.parser.StoredProcedureParser;
import com.example.shelldemo.parser.StoredProcedureParser.ProcedureParam;
import com.example.shelldemo.connection.DatabaseConnectionFactory;
import com.example.shelldemo.config.DatabaseProperties;

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
    private static final Logger logger = LoggerFactory.getLogger(UnifiedDatabaseRunner.class);
    private static final Logger methodLogger = LoggerFactory.getLogger(UnifiedDatabaseRunner.class.getName() + ".methods");
    private final ConnectionConfig config;
    private UnifiedDatabaseOperation dbOperation;
    private final DatabaseConnectionFactory connectionFactory;
    private final DatabaseProperties dbProperties;

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

    public UnifiedDatabaseRunner() throws IOException {
        logger.trace("Initializing UnifiedDatabaseRunner");
        this.config = new ConnectionConfig();
        this.connectionFactory = new DatabaseConnectionFactory(new DatabaseProperties());
        this.dbProperties = new DatabaseProperties();
        logger.debug("UnifiedDatabaseRunner initialized successfully");
    }

    private void initializeConfig() {
        logger.debug("Initializing database configuration");
        methodLogger.debug("[initializeConfig] Starting with parameters - type: {}, host: {}, port: {}, database: {}", 
            dbType, host, port > 0 ? port : getDefaultPort(), database);
        
        config.setHost(host);
        config.setUsername(username);
        config.setPassword("********"); // Mask password in logs
        config.setServiceName(database);
        config.setPort(port > 0 ? port : getDefaultPort());
        
        String validatedDbType = dbType.trim().toLowerCase();
        if (!isValidDbType(validatedDbType)) {
            String errorMessage = "Invalid database type: " + validatedDbType;
            logger.error(errorMessage);
            methodLogger.error("[initializeConfig] Validation failed for database type: {}", validatedDbType);
            throw new IllegalArgumentException(errorMessage);
        }

        if (validatedDbType.equals("oracle")) {
            if (connectionType == null || (!connectionType.equals("thin") && !connectionType.equals("ldap"))) {
                logger.info("No connection type specified for Oracle, defaulting to LDAP");
                methodLogger.debug("[initializeConfig] Setting default Oracle connection type to LDAP");
                connectionType = "ldap";
            }
            config.setConnectionType(connectionType);
            methodLogger.debug("[initializeConfig] Oracle connection configured with type: {}", connectionType);
        }

        this.dbOperation = new UnifiedDatabaseOperation(validatedDbType, config, connectionFactory);
        logger.info("Database configuration initialized successfully for type: {}", validatedDbType);
        methodLogger.debug("[initializeConfig] Configuration completed successfully");
    }

    private int executeScript(UnifiedDatabaseOperation dbOperation, File scriptFile) throws IOException, SQLException {
        logger.info("Executing SQL script: {}", scriptFile.getAbsolutePath());
        methodLogger.debug("[executeScript] Starting execution of script: {}", scriptFile.getName());
        
        List<String> statements = dbOperation.parseSqlFile(scriptFile);
        logger.debug("Found {} SQL statements in script", statements.size());
        methodLogger.debug("[executeScript] Parsed {} SQL statements from file", statements.size());
        
        int statementCount = 0;
        for (String sql : statements) {
            statementCount++;
            methodLogger.debug("[executeScript] Processing statement {}/{}", statementCount, statements.size());
            methodLogger.trace("[executeScript] SQL content: {}", sql);
            
            if (printStatements) {
                logger.info("Executing SQL: {}", sql);
            } else {
                logger.debug("Executing SQL statement (length: {})", sql.length());
            }
            
            dbOperation.executeUpdate(sql);
            methodLogger.debug("[executeScript] Statement {}/{} executed successfully", statementCount, statements.size());
        }
        
        logger.info("Script execution completed successfully - {} statements executed", statements.size());
        methodLogger.debug("[executeScript] Completed execution of all statements");
        return 0;
    }

    private int executeStoredProcedure(UnifiedDatabaseOperation dbOperation, List<ProcedureParam> params) throws SQLException {
        logger.info("Executing stored procedure: {}", target);
        methodLogger.debug("[executeStoredProcedure] Starting execution - Name: {}, Function: {}, Params: {}", 
            target, isFunction, params.size());
        
        if (isFunction) {
            methodLogger.debug("[executeStoredProcedure] Executing as function with return type: {}", returnType);
            Object result = dbOperation.callStoredProcedure(target, params.toArray());
            logger.info("Function execution successful, result: {}", result);
            methodLogger.debug("[executeStoredProcedure] Function completed with result: {}", result);
        } else {
            methodLogger.debug("[executeStoredProcedure] Executing as procedure with {} parameters", params.size());
            dbOperation.callStoredProcedure(target, params.toArray());
            logger.info("Procedure execution successful");
            methodLogger.debug("[executeStoredProcedure] Procedure completed successfully");
        }
        return 0;
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
            loadDriverFromPath(driverPath);
        }

        try {
            initializeConfig();
            File scriptFile = new File(target);

            if (scriptFile.exists()) {
                methodLogger.debug("[call] Executing as script file: {}", scriptFile.getAbsolutePath());
                return executeScript(dbOperation, scriptFile);
            } else {
                methodLogger.debug("[call] Executing as stored procedure: {}", target);
                return executeStoredProcedure(dbOperation, parseParameters());
            }
        } catch (Exception e) {
            logger.error("Operation failed: {}", e.getMessage(), e);
            methodLogger.error("[call] Execution failed with error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isValidDbType(String dbType) {
        logger.trace("Validating database type: {}", dbType);
        return dbProperties.isValidDbType(dbType);
    }

    private int getDefaultPort() {
        logger.trace("Getting default port for database type: {}", dbType);
        return dbProperties.getDefaultPort(dbType);
    }

    private void loadDriverFromPath(String path) {
        logger.debug("Loading JDBC driver from path: {}", path);
        try {
            File driverFile = new File(path);
            if (!driverFile.exists()) {
                logger.error("Driver file not found: {}", path);
                return;
            }

            URL driverUrl = driverFile.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{driverUrl}, getClass().getClassLoader());
            logger.debug("Created URLClassLoader for driver path: {}", path);

            // Try to load the driver using ServiceLoader
            ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class, loader);
            for (Driver driver : drivers) {
                logger.info("Registering JDBC driver: {}", driver.getClass().getName());
                DriverManager.registerDriver(new CustomDriver(driver));
            }
            logger.debug("Driver loading completed successfully");
        } catch (Exception e) {
            logger.error("Failed to load driver from path: {} - {}", path, e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        try {
            logger.info("Starting UnifiedDatabaseRunner");
            int exitCode = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
            logger.info("UnifiedDatabaseRunner completed with exit code: {}", exitCode);
            System.exit(exitCode);
        } catch (IOException e) {
            logger.error("Failed to initialize database configuration: {}", e.getMessage(), e);
            System.err.println("Failed to initialize database configuration: " + e.getMessage());
            System.exit(1);
        }
    }
}