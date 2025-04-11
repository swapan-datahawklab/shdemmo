package com.example.shelldemo.service.database;

import com.example.shelldemo.UnifiedDatabaseOperation;
import com.example.shelldemo.config.DatabaseProperties;
import com.example.shelldemo.config.YamlConfigReader;
import com.example.shelldemo.model.entity.config.ConnectionConfig;
import com.example.shelldemo.model.dto.response.ConnectionTestResult;
import com.example.shelldemo.connection.DatabaseConnectionFactory;
import com.example.shelldemo.exception.DatabaseOperationException;
import com.example.shelldemo.exception.DatabaseConnectionException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseLoginService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseLoginService.class);
    private static final String DEFAULT_CONFIG_PATH = "application.yaml";
    private static final String DB_LIST_PATH = "dblist.yaml";
    private static final int DEFAULT_THREAD_COUNT = 10;
    private static final String UNKNOWN = "unknown";
    
    private final YamlConfigReader configReader;
    private final DatabaseProperties dbProperties;
    private final DatabaseConnectionFactory connectionFactory;
    private final int threadCount;

    public DatabaseLoginService() throws IOException {
        this(DEFAULT_THREAD_COUNT);
    }

    public DatabaseLoginService(int threadCount) throws IOException {
        this.configReader = new YamlConfigReader(DEFAULT_CONFIG_PATH);
        this.dbProperties = new DatabaseProperties();
        this.connectionFactory = new DatabaseConnectionFactory(dbProperties);
        this.threadCount = threadCount;
    }

    public void runTests(String outputFile) throws IOException {
        logger.info("Starting database tests with {} virtual threads", threadCount);
        
        // Read LDAP configuration
        Map<String, Object> config = configReader.readConfig(DEFAULT_CONFIG_PATH, new TypeReference<>() {});
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), new TypeReference<>() {});
        
        // Read database list
        Map<String, Object> dbList = configReader.readConfig(DB_LIST_PATH, new TypeReference<>() {});
        List<Map<String, String>> databases = configReader.convertValue(dbList.get("databases"), new TypeReference<>() {});

        // Create virtual thread executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<ConnectionTestResult> results = databases.stream()
                .map(db -> executor.submit(() -> testDatabase(db, ldapConfig)))
                .toList()
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Database test interrupted", e);
                        return new ConnectionTestResult.Builder()
                            .databaseName(UNKNOWN)
                            .serviceName(UNKNOWN)
                            .dbType(UNKNOWN)
                            .success(false)
                            .error("Test interrupted", "INTERRUPTED")
                            .build();
                    } catch (Exception e) {
                        logger.error("Error executing database test", e);
                        return new ConnectionTestResult.Builder()
                            .databaseName(UNKNOWN)
                            .serviceName(UNKNOWN)
                            .dbType(UNKNOWN)
                            .success(false)
                            .error(e.getMessage(), "ERROR")
                            .build();
                    }
                })
                .toList();

            // Write results to CSV
            writeResultsToCsv(results, outputFile);
        }
    }

    private ConnectionTestResult testDatabase(Map<String, String> db, Map<String, Object> ldapConfig) {
        String dbName = db.get("name");
        String serviceName = db.get("serviceName");
        String dbType = db.get("type");
        long startTime = System.currentTimeMillis();
        
        try {
            // Create connection config
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setHost((String) ldapConfig.get("host"));
            connectionConfig.setPort((Integer) ldapConfig.get("port"));
            connectionConfig.setServiceName(serviceName);
            
            // Get credentials from properties
            connectionConfig.setUsername(dbProperties.getProperty("db." + dbType + ".username"));
            connectionConfig.setPassword(dbProperties.getProperty("db." + dbType + ".password"));

            // Run database test using UnifiedDatabaseOperation
            try (UnifiedDatabaseOperation operation = new UnifiedDatabaseOperation(dbType, connectionConfig, connectionFactory)) {
                List<Map<String, Object>> results = operation.executeQuery("SELECT 1 FROM DUAL");
                long responseTime = System.currentTimeMillis() - startTime;
                
                return new ConnectionTestResult.Builder()
                    .databaseName(dbName)
                    .serviceName(serviceName)
                    .dbType(dbType)
                    .success(!results.isEmpty())
                    .responseTimeMs(responseTime)
                    .build();
            } catch (DatabaseOperationException | DatabaseConnectionException e) {
                long responseTime = System.currentTimeMillis() - startTime;
                logger.error("Error testing database {}: {}", dbName, e.getMessage());
                
                return new ConnectionTestResult.Builder()
                    .databaseName(dbName)
                    .serviceName(serviceName)
                    .dbType(dbType)
                    .success(false)
                    .responseTimeMs(responseTime)
                    .error(e.getMessage(), e.getCause() instanceof SQLException ? ((SQLException) e.getCause()).getSQLState() : "ERROR")
                    .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error testing database {}: {}", dbName, e.getMessage());
            
            return new ConnectionTestResult.Builder()
                .databaseName(dbName)
                .serviceName(serviceName)
                .dbType(dbType)
                .success(false)
                .responseTimeMs(responseTime)
                .error(e.getMessage(), "ERROR")
                .build();
        }
    }

    private void writeResultsToCsv(List<ConnectionTestResult> results, String outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(ConnectionTestResult.getCsvHeader() + "\n");
            for (ConnectionTestResult result : results) {
                writer.write(result.toCsvRecord() + "\n");
            }
        }
    }
} 