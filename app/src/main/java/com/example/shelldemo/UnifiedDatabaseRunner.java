package com.example.shelldemo;

import java.io.File;
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

import com.example.shelldemo.vault.VaultSecretFetcherBuilder;

@Command(name = "db", mixinStandardHelpOptions = true, version = "1.0",description = "Unified Database CLI Tool")
public class UnifiedDatabaseRunner implements Callable<Integer> {
    private static final Logger logger = LogManager.getLogger(UnifiedDatabaseRunner.class);
    
    @Option(names = {"-t", "--type"}, required = true,description = "Database type (oracle, sqlserver, postgresql, mysql)")
    private String dbType;

    @Option(names = {"--connection-type"}, description = "Connection type for Oracle (thin, ldap). Defaults to ldap if not specified.")
    private String connectionType;

    @Option(names = {"-H", "--host"}, description = "Database host")
    private String host;

    @Option(names = {"-P", "--port"}, description = "Database port (defaults: oracle=1521, sqlserver=1433, postgresql=5432, mysql=3306)")
    private int port;

    @Option(names = {"-u", "--username"}, required = true, description = "Database username")
    private String username;

    @Option(names = {"-p", "--password"}, description = "Database password")
    private String password;

    @Option(names = {"-d", "--database"}, required = true, description = "Database name")
    private String database;

    @Option(names = {"--stop-on-error"}, defaultValue = "true",description = "Stop execution on error")
    private boolean stopOnError;

    @Option(names = {"--auto-commit"}, defaultValue = "false",description = "Auto-commit mode")
    private boolean autoCommit;

    @Option(names = {"--print-statements"}, defaultValue = "false",description = "Print SQL statements")
    private boolean printStatements;

    @Parameters(index = "0", paramLabel = "TARGET", description = "SQL script file or stored procedure name", arity = "0..1")
    private String target;

    @Option(names = {"--function"}, description = "Execute as function")
    private boolean isFunction;

    @Option(names = {"--return-type"}, defaultValue = "NUMERIC",description = "Return type for functions")
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

    @Option(names = {"--transactional"}, defaultValue = "false", description = "Execute DML statements in a transaction (default: false)")
    private boolean transactional;

    @Option(names = {"--show-connect-string"}, description = "Show the generated JDBC connection string and exit")
    private boolean showConnectString;

    @Option(names = {"--secret"}, description = "Fetch Oracle password from Vault using secret name (mutually exclusive with -p/--password)")
    private String secretName;

    @Override
    public Integer call() throws Exception {
        logger.info("Starting database operation - type: {}, target: {}", dbType, target);

        if (secretName != null && password != null && !password.trim().isEmpty()) {
            logger.error("--secret and -p/--password are mutually exclusive. Please specify only one.");
            return 2;
        }

        if (showConnectString) {
            return showConnectString();
        }

        if (target == null || target.trim().isEmpty()) {
            logger.error("Target file or procedure name is required");
            return 2;
        }

        if (driverPath != null) {
            logger.info("Loading custom JDBC driver from: {}", driverPath);
        }

        if (secretName != null) {
            try {
                password = fetchPasswordFromVault(secretName);
            } catch (Exception e) {
                logger.error("Failed to fetch password from Vault: {}", e.getMessage());
                return 2;
            }
        }

        if ((password == null || password.trim().isEmpty()) && secretName == null) {
            password = promptForPassword();
        }

        return runDatabaseOperation();
    }

    private int showConnectString() {
        com.example.shelldemo.connection.ConnectionConfig connConfig = new com.example.shelldemo.connection.ConnectionConfig();
        connConfig.setDbType(dbType);
        connConfig.setHost(host);
        connConfig.setPort(port);
        connConfig.setUsername(username);
        connConfig.setPassword(password);
        connConfig.setServiceName(database);
        connConfig.setConnectionType(connectionType);
        String connectString = new com.example.shelldemo.connection.DatabaseConnectionFactory().buildConnectionUrl(connConfig);
        logger.info(connectString);
        if ("thin".equalsIgnoreCase(connectionType) && (host == null || host.trim().isEmpty())) {
            logger.error("Host is required for connection type 'thin'");
            return 2;
        }
        return 0;
    }

    private String promptForPassword() {
        System.out.print("Enter database password: ");
        java.io.Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword();
            if (pwd != null) return new String(pwd);
        } else {
            try (java.util.Scanner scanner = new java.util.Scanner(System.in)) {
                return scanner.nextLine();
            }
        }
        return "";
    }

    private int runDatabaseOperation() {
        try (UnifiedDatabaseOperation operation = UnifiedDatabaseOperation.builder()
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .dbType(dbType)
                .serviceName(database)
                .connectionType(connectionType)
                .build()
            ) {
            File scriptFile = new File(target);

            if (scriptFile.isDirectory()) {
                logger.error("Target '{}' is a directory, expected a file or procedure name", target);
                return 2;
            }

            if (!scriptFile.exists()) {
                if (target.contains("/") || target.contains("\\")) {
                    logger.error("File not found: {}", target);
                    return 2;
                }
                logger.debug("Executing as stored procedure: {}", target);
                operation.executeStoredProcedure(target, isFunction);
                return 0;
            }

            if (preFlight) {
                operation.getStatementExecutor().validateScript(scriptFile.getPath(), showExplainPlan);
                return 0;
            }

            logger.debug("Executing as script file: {}", scriptFile.getAbsolutePath());
            operation.executeScript(scriptFile, transactional);
            return 0;
        } catch (Exception e) {
            logger.error("Operation failed: {}", e.getMessage(), e);
            return 1;
        }
    }

    private String fetchPasswordFromVault(String secretName) throws Exception {
        var config = com.example.shelldemo.config.ConfigurationHolder.getInstance();
        var vaultConfig = config.getDatabaseConfig("vault");
        String vaultBaseUrl = (String) vaultConfig.get("baseUrl");
        String roleId = (String) vaultConfig.get("roleId");
        String secretId = (String) vaultConfig.get("secretId");
        String ait = (String) vaultConfig.get("ait");
        return new VaultSecretFetcherBuilder()
            .build()
            .fetchOraclePassword(
                vaultBaseUrl, roleId, secretId, secretName, ait
            );
    }

    public static void main(String[] args) {
        // Configure Log4j programmatically
        ConfigurationBuilder<BuiltConfiguration> log4jConfigBuilder = ConfigurationBuilderFactory.newConfigurationBuilder();
        
        // Create appenders
        log4jConfigBuilder.add(log4jConfigBuilder.newAppender("Console", "CONSOLE").addAttribute("target", "SYSTEM_OUT")
                                                 .add(log4jConfigBuilder.newLayout("PatternLayout")
                                                 .addAttribute("pattern", "%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")));
        
        // Create root logger
        log4jConfigBuilder.add(log4jConfigBuilder.newRootLogger(org.apache.logging.log4j.Level.INFO)
                                                 .add(log4jConfigBuilder.newAppenderRef("Console")));
        // Configure async logging
        log4jConfigBuilder.add(log4jConfigBuilder.newAsyncLogger("com.example.shelldemo", org.apache.logging.log4j.Level.DEBUG)
                                                 .addAttribute("includeLocation", "true"));
        // Initialize Log4j
        Configurator.initialize(log4jConfigBuilder.build());
        
        logger.info("Starting UnifiedDatabaseRunner...");
        int exitCode = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        logger.info("UnifiedDatabaseRunner completed with exit code: {}", exitCode);
        System.exit(exitCode);
    }
}