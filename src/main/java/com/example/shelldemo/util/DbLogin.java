package com.example.shelldemo.util;

import com.example.shelldemo.model.ConnectionConfig;
import com.example.shelldemo.model.DatabaseResult;
import com.example.shelldemo.config.OracleConnectionGenerator;
import com.example.shelldemo.config.YamlConfigReader;
import com.example.shelldemo.datasource.UnifiedDatabaseOperation;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DbLogin {
    private static final Logger logger = LogManager.getLogger(DbLogin.class);
    private static final String DEFAULT_CONFIG_PATH = "application.yaml";
    private static final String DB_LIST_PATH = "dblist.yaml";
    private static final int DEFAULT_THREAD_COUNT = 10;
    
    private final YamlConfigReader configReader;
    private final OracleConnectionGenerator connectionGenerator;
    private final int threadCount;

    public DbLogin() {
        this(DEFAULT_THREAD_COUNT);
    }

    public DbLogin(int threadCount) {
        this.configReader = new YamlConfigReader();
        this.connectionGenerator = new OracleConnectionGenerator();
        this.threadCount = threadCount;
    }

    public void runTests(String outputFile) throws IOException {
        logger.info("Starting database tests with {} virtual threads", threadCount);
        
        // Read LDAP configuration
        Map<String, Object> config = configReader.readConfig(DEFAULT_CONFIG_PATH, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> ldapConfig = configReader.convertValue(config.get("ldap"), new TypeReference<Map<String, Object>>() {});
        
        // Read database list
        Map<String, Object> dbList = configReader.readConfig(DB_LIST_PATH, new TypeReference<Map<String, Object>>() {});
        List<Map<String, String>> databases = configReader.convertValue(dbList.get("databases"), new TypeReference<List<Map<String, String>>>() {});

        // Create virtual thread executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<DatabaseResult> results = databases.stream()
                .map(db -> executor.submit(() -> testDatabase(db, ldapConfig)))
                .collect(Collectors.toList())
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        logger.error("Error executing database test", e);
                        return new DatabaseResult("unknown", "unknown", false);
                    }
                })
                .collect(Collectors.toList());

            // Write results to CSV
            writeResultsToCsv(results, outputFile);
        }
    }

    private DatabaseResult testDatabase(Map<String, String> db, Map<String, Object> ldapConfig) {
        String dbName = db.get("name");
        String serviceName = db.get("serviceName");
        
        try {
            // Create connection config
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.setHost((String) ldapConfig.get("host"));
            connectionConfig.setPort((Integer) ldapConfig.get("port"));
            connectionConfig.setServiceName(serviceName);
            connectionConfig.setUsername(connectionGenerator.getUsername());
            connectionConfig.setPassword(connectionGenerator.getPassword());

            // Run database test using UnifiedDatabaseRunner
            UnifiedDatabaseOperation operation = UnifiedDatabaseOperation.create(
                connectionConfig.getDatabaseType(),
                connectionConfig
            );
            List<Map<String, Object>> results = operation.executeQuery("SELECT 1 FROM DUAL");
            boolean success = !results.isEmpty();
            return new DatabaseResult(dbName, serviceName, success);
            
        } catch (Exception e) {
            logger.error("Error testing database {}: {}", dbName, e.getMessage());
            return new DatabaseResult(dbName, serviceName, false);
        }
    }

    private void writeResultsToCsv(List<DatabaseResult> results, String outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("Database,ServiceName,Status\n");
            for (DatabaseResult result : results) {
                writer.write(String.format("%s,%s,%s\n",
                    result.getDatabaseName(),
                    result.getServiceName(),
                    result.isSuccess() ? "PASS" : "FAIL"));
            }
        }
    }
} 