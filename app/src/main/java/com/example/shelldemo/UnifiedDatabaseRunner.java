package com.example.shelldemo;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import com.example.shelldemo.connection.ConnectionConfig;
import com.example.shelldemo.parser.storedproc.StoredProcedureParser;
import com.example.shelldemo.parser.storedproc.ProcedureParam;


/**
 * A unified database runner that combines CLI and script execution functionality.
 * Supports executing SQL scripts and stored procedures with proper error handling.
 */
@Command(name = "db", mixinStandardHelpOptions = true, version = "1.0",
    description = "Unified Database CLI Tool")
public class UnifiedDatabaseRunner implements Callable<Integer> {
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseRunner.class);
    private UnifiedDatabaseOperation dbOperation;
    private final DatabaseOperationFactory operationFactory;
    
    // Add a factory interface
    @FunctionalInterface
    public interface DatabaseOperationFactory {
        UnifiedDatabaseOperation create(String dbType, ConnectionConfig config) throws IOException;
    }
    
    // Default constructor for CLI use
    public UnifiedDatabaseRunner() {
        this(UnifiedDatabaseOperation::create);
    }
    
    // Test constructor
    public UnifiedDatabaseRunner(DatabaseOperationFactory factory) {
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

    @Option(names = {"--pre-flight"}, description = "Validate statements without executing them")
    private boolean preFlight;

    @Option(names = {"--validate-script"}, description = "Show execution plan and validate syntax for each statement during pre-flight")
    private boolean showExplainPlan;

    private List<ProcedureParam> parseParameters() {
        logger.debug("Starting parameter parsing");
        List<ProcedureParam> params = StoredProcedureParser.parse(inputParams, outputParams, ioParams);
        logger.debug("Successfully parsed {} parameters", params.size());
        return params;
    }

    private void validateStatements(File scriptFile) throws IOException, SQLException {
        logger.info("Starting pre-flight validation of {}", scriptFile.getName());
        dbOperation.validateScript(scriptFile, showExplainPlan);
    }

    @Override
    public Integer call() throws Exception {
        logger.info("Starting database operation - type: {}, target: {}", dbType, target);
        
        if (driverPath != null) {
            logger.info("Loading custom JDBC driver from: {}", driverPath);
        }

        ConnectionConfig config = new ConnectionConfig();
        config.setHost(host);
        config.setUsername(username);
        config.setPassword(password);
        config.setServiceName(database);
        config.setPort(port);
        config.setDbType(dbType);
        config.setConnectionType(connectionType);

        try (UnifiedDatabaseOperation operation = operationFactory.create(dbType, config)) {
            this.dbOperation = operation;
            File scriptFile = new File(target);

            if (!scriptFile.exists()) {
                logger.debug("Executing as stored procedure: {}", target);
                operation.executeStoredProcedure(target, isFunction, parseParameters().toArray());
                return 0;
            }

            if (preFlight) {
                validateStatements(scriptFile);
                return 0;
            }

            logger.debug("Executing as script file: {}", scriptFile.getAbsolutePath());
            operation.executeScript(scriptFile, printStatements);
            return 0;
        } catch (Exception e) {
            logger.error("Operation failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public static void main(String[] args) {
        // Configure Log4j programmatically
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        
        // Create appenders
        builder.add(builder.newAppender("Console", "CONSOLE")
            .addAttribute("target", "SYSTEM_OUT")
            .add(builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")));
        
        // Create root logger
        builder.add(builder.newRootLogger(org.apache.logging.log4j.Level.INFO)
            .add(builder.newAppenderRef("Console")));
        
        // Configure async logging
        builder.add(builder.newAsyncLogger("com.example.shelldemo", org.apache.logging.log4j.Level.DEBUG)
            .addAttribute("includeLocation", "true"));
        
        // Initialize Log4j
        Configurator.initialize(builder.build());
        
        logger.info("Starting UnifiedDatabaseRunner...");
        int exitCode = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        logger.info("UnifiedDatabaseRunner completed with exit code: {}", exitCode);
        System.exit(exitCode);
    }
}