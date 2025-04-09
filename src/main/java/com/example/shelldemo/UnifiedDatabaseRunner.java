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
import com.example.shelldemo.model.domain.ConnectionConfig;
import com.example.shelldemo.util.ProcScriptParser;
import com.example.shelldemo.util.ProcScriptParser.ProcedureParam;

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
    private static final Logger log = LoggerFactory.getLogger(UnifiedDatabaseRunner.class);
    private final ConnectionConfig config;
    private UnifiedDatabaseOperation dbOperation;

    @Option(names = {"-t", "--type"}, required = true,
        description = "Database type (oracle, sqlserver, postgresql, mysql)")
    private String dbType;

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

    public UnifiedDatabaseRunner() {
        this.config = new ConnectionConfig();
    }

    private void initializeConfig() {
        config.setHost(host);
        config.setUsername(username);
        config.setPassword(password);
        config.setServiceName(database);
        config.setPort(port > 0 ? port : getDefaultPort());
        
        String validatedDbType = dbType.trim().toLowerCase();
        if (!isValidDbType(validatedDbType)) {
            throw new IllegalArgumentException("Invalid database type: " + validatedDbType);
        }
        this.dbOperation = new UnifiedDatabaseOperation(validatedDbType, config);
    }

    private int executeScript(UnifiedDatabaseOperation dbOperation, File scriptFile) throws IOException, SQLException {
        List<String> statements = dbOperation.parseSqlFile(scriptFile);
        for (String sql : statements) {
            if (printStatements) {
                log.info("Executing: {}", sql);
            }
            dbOperation.executeUpdate(sql);
        }
        return 0;
    }

    private int executeStoredProcedure(UnifiedDatabaseOperation dbOperation, List<ProcedureParam> params) throws SQLException {
        if (isFunction) {
            Object result = dbOperation.callStoredProcedure(target, params.toArray());
            log.info("Function result: {}", result);
        } else {
            dbOperation.callStoredProcedure(target, params.toArray());
        }
        return 0;
    }

    private List<ProcedureParam> parseParameters() {
        return ProcScriptParser.parse(inputParams, outputParams, ioParams);
    }

    @Override
    public Integer call() throws Exception {
        log.debug("Starting UnifiedDatabaseRunner with parameters:");
        log.debug("dbType: {}", dbType);
        log.debug("host: {}", host);
        log.debug("port: {}", port);
        log.debug("username: {}", username);
        log.debug("database: {}", database);
        
        if (driverPath != null) {
            loadDriverFromPath(driverPath);
        }

        try {
            initializeConfig();
            File scriptFile = new File(target);

            if (scriptFile.exists()) {
                return executeScript(dbOperation, scriptFile);
            } else {
                return executeStoredProcedure(dbOperation, parseParameters());
            }
        } catch (Exception e) {
            if (e instanceof SQLException sqlexception) {
                log.error(formatOracleError(sqlexception));
            } else {
                log.error("Error: {}", e.getMessage());
            }
            return 1;
        }
    }

    private boolean isValidDbType(String dbType) {
        return dbType.equals("oracle") || 
               dbType.equals("sqlserver") || 
               dbType.equals("postgresql") || 
               dbType.equals("mysql");
    }

    private int getDefaultPort() {
        switch (dbType.toLowerCase()) {
            case "oracle":
                return 1521;
            case "sqlserver":
                return 1433;
            case "postgresql":
                return 5432;
            case "mysql":
                return 3306;
            default:
                return 1521;
        }
    }

    private String formatOracleError(SQLException e) {
        String message = e.getMessage();
        int oraIndex = message.indexOf("ORA-");
        if (oraIndex >= 0) {
            int endIndex = message.indexOf(":", oraIndex);
            String oraCode = endIndex > oraIndex ? message.substring(oraIndex, endIndex) : message.substring(oraIndex);
            String errorDetails = message.substring(endIndex + 1).trim();
            
            // Extract the most relevant part of the error message
            int detailsIndex = errorDetails.indexOf("ORA-03301");
            if (detailsIndex > 0) {
                errorDetails = errorDetails.substring(0, detailsIndex).trim();
            }
            
            return oraCode + ": " + errorDetails;
        }
        return message;
    }

   

    private void loadDriverFromPath(String path) {
        try {
            File driverFile = new File(path);
            if (!driverFile.exists()) {
                log.error("Driver file not found: {}", path);
                return;
            }

            URL driverUrl = driverFile.toURI().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[]{driverUrl}, getClass().getClassLoader());

            // Try to load the driver using ServiceLoader
            ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class, loader);
            for (Driver driver : drivers) {
                log.info("Loaded driver: {}", driver.getClass().getName());
                DriverManager.registerDriver(new CustomDriver(driver));
            }
        } catch (Exception e) {
            log.error("Error loading driver f`rom path: {}", e.getMessage());
        }
    }


    public static void main(String[] args) {
        int exitCode = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        System.exit(exitCode);
    }
}