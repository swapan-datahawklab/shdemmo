package com.example.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.datasource.UnifiedDatabaseOperation;
import com.example.shelldemo.model.ConnectionConfig;

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
    
    private UnifiedDatabaseOperation dbOperation;
    private final ConnectionConfig config;
    
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

    public UnifiedDatabaseRunner() {
        this.config = new ConnectionConfig();
    }
    
    @Override
    public Integer call() {
        try {
            // Initialize config
            config.setHost(host);
            config.setPort(getDefaultPort());
            config.setUsername(username);
            config.setPassword(password);
            config.setDatabase(database);
            
            // Set database type and load driver
            this.dbType = dbType.toLowerCase();
            if (driverPath != null) {
                loadDriverFromPath(driverPath);
            }
            
            // Create database operation directly
            dbOperation = new UnifiedDatabaseOperation(dbType, config);
            
            // Determine operation type
            File scriptFile = new File(target);
            if (scriptFile.exists()) {
                return runScript(scriptFile);
            } else {
                return runStoredProc();
            }
        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage());
            return 1;
        }
    }
    
    private int getDefaultPort() {
        if (port > 0) {
            return port;
        }
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
    
    private int runScript(File scriptFile) throws SQLException {
        return dbOperation.execute(conn -> {
            conn.setAutoCommit(autoCommit);
            
            try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
                StringBuilder currentStatement = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    // Skip comments and empty lines
                    if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                        continue;
                    }
                    
                    currentStatement.append(line).append("\n");
                    
                    // Execute when we find a semicolon
                    if (line.trim().endsWith(";")) {
                        executeStatement(conn, currentStatement.toString().trim());
                        currentStatement.setLength(0);
                    }
                }
                
                // Execute any remaining statement
                if (currentStatement.length() > 0) {
                    executeStatement(conn, currentStatement.toString().trim());
                }
            } catch (IOException e) {
                log.error("Error reading script file: {}", e.getMessage());
                throw new SQLException("Failed to read script file: " + e.getMessage(), e);
            }
            return 0;
        });
    }
    
    private void executeStatement(Connection conn, String sql) throws SQLException {
        if (printStatements) {
            log.info("Executing: {}", sql);
        }
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            if (!autoCommit) {
                conn.commit();
            }
        } catch (SQLException e) {
            if (!autoCommit) {
                conn.rollback();
            }
            if (stopOnError) {
                throw e;
            }
            log.error("Error executing statement: {}", e.getMessage());
        }
    }
    
    private int runStoredProc() throws SQLException {
        return dbOperation.execute(conn -> {
            List<ProcedureParam> params = parseParameters();
            
            if (isFunction) {
                return runFunction(conn, params);
            } else {
                return runProcedure(conn, params);
            }
        });
    }
    
    private List<ProcedureParam> parseParameters() {
        List<ProcedureParam> params = new ArrayList<>();
        
        // Add input parameters
        if (inputParams != null) {
            String[] paramPairs = inputParams.split(",");
            for (String paramPair : paramPairs) {
                String[] parts = paramPair.split(":");
                params.add(new ProcedureParam(parts[0], parts[1], parseValue(parts[2])));
            }
        }
        
        // Add output parameters
        if (outputParams != null) {
            String[] paramPairs = outputParams.split(",");
            for (String paramPair : paramPairs) {
                String[] parts = paramPair.split(":");
                params.add(new ProcedureParam(parts[0], parts[1], null));
            }
        }
        
        // Add input/output parameters
        if (ioParams != null) {
            String[] paramPairs = ioParams.split(",");
            for (String paramPair : paramPairs) {
                String[] parts = paramPair.split(":");
                params.add(new ProcedureParam(parts[0], parts[1], parseValue(parts[2])));
            }
        }
        
        return params;
    }
    
    private int runFunction(Connection conn, List<ProcedureParam> params) throws SQLException {
        if (returnType == null) {
            log.error("Return type must be specified for functions");
            return 1;
        }
        
        String call = buildCallString(target, true, params.size());
        try (CallableStatement stmt = conn.prepareCall(call)) {
            // Register return parameter
            stmt.registerOutParameter(1, getSqlType(returnType));
            
            // Register parameters
            int paramIndex = 2;
            for (ProcedureParam param : params) {
                stmt.setObject(paramIndex++, param.getValue());
            }
            
            // Execute the function
            stmt.execute();
            Object result = stmt.getObject(1);
            log.info("Function result: {}", result);
            return 0;
        }
    }
    
    private int runProcedure(Connection conn, List<ProcedureParam> params) throws SQLException {
        String call = buildCallString(target, false, params.size());
        try (CallableStatement stmt = conn.prepareCall(call)) {
            // Register parameters
            int paramIndex = 1;
            for (ProcedureParam param : params) {
                stmt.setObject(paramIndex++, param.getValue());
            }
            
            // Execute the procedure
            stmt.execute();
            printOutputParams(stmt, params);
            return 0;
        }
    }
    
    private String buildCallString(String procedureName, boolean isFunction, int paramCount) {
        StringBuilder call = new StringBuilder();
        
        if (isFunction) {
            call.append("{? = call ").append(procedureName).append("(");
        } else {
            call.append("{call ").append(procedureName).append("(");
        }
        
        for (int i = 0; i < paramCount; i++) {
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
    
    private Object parseValue(String value) {
        // Simple value parsing - can be enhanced based on needs
        return value;
    }
    
    private void printOutputParams(CallableStatement stmt, List<ProcedureParam> params) throws SQLException {
        int paramIndex = 1;
        for (ProcedureParam param : params) {
            if (param.getValue() == null) { // Output parameter
                Object value = stmt.getObject(paramIndex);
                log.info("{} = {}", param.getName(), value);
            }
            paramIndex++;
        }
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
                DriverManager.registerDriver(new DriverShim(driver));
            }
        } catch (Exception e) {
            log.error("Error loading driver from path: {}", e.getMessage());
        }
    }
    
    private static class DriverShim implements Driver {
        private final Driver driver;
        
        DriverShim(Driver driver) {
            this.driver = driver;
        }
        
        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }
        
        @Override
        public Connection connect(String url, java.util.Properties info) throws SQLException {
            return driver.connect(url, info);
        }
        
        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }
        
        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }
        
        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }
        
        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }
        
        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
    
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
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new UnifiedDatabaseRunner()).execute(args);
        System.exit(exitCode);
    }
}
 