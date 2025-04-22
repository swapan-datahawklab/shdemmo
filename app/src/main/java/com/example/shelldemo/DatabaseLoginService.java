package com.example.shelldemo;

import com.example.shelldemo.model.dto.response.ConnectionTestResult;
import com.example.shelldemo.exception.DatabaseConnectionException;
import com.example.shelldemo.exception.DatabaseOperationException;

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
    private static final int DEFAULT_THREAD_COUNT = 10;
    
    private final int threadCount;

    public DatabaseLoginService() {
        this(DEFAULT_THREAD_COUNT);
    }

    public DatabaseLoginService(int threadCount) {
        this.threadCount = threadCount;
    }

    public void runTests(String outputFile) throws IOException {
        logger.info("Starting database tests with {} virtual threads", threadCount);
        
        // Create virtual thread executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ConnectionTestResult result = executor.submit(() -> testDatabase()).get();
            List<ConnectionTestResult> results = List.of(result);
            writeResultsToCsv(results, outputFile);
        } catch (Exception e) {
            logger.error("Error running database tests", e);
            throw new IOException("Failed to run database tests", e);
        }
    }

    private ConnectionTestResult testDatabase() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Run database test using UnifiedDatabaseOperation
            try (UnifiedDatabaseOperation operation = UnifiedDatabaseOperation.create("oracle", null)) {
                List<Map<String, Object>> results = operation.executeQuery(operation.getTestQuery());
                long responseTime = System.currentTimeMillis() - startTime;
                
                return new ConnectionTestResult.Builder()
                    .databaseName("test")
                    .serviceName("test")
                    .dbType("oracle")
                    .success(!results.isEmpty())
                    .responseTimeMs(responseTime)
                    .build();
            } catch (DatabaseOperationException | DatabaseConnectionException e) {
                long responseTime = System.currentTimeMillis() - startTime;
                logger.error("Error testing database: {}", e.getMessage());
                
                return new ConnectionTestResult.Builder()
                    .databaseName("test")
                    .serviceName("test")
                    .dbType("oracle")
                    .success(false)
                    .responseTimeMs(responseTime)
                    .error(e.getMessage(), e.getCause() instanceof SQLException ? ((SQLException) e.getCause()).getSQLState() : "ERROR")
                    .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error testing database: {}", e.getMessage());
            
            return new ConnectionTestResult.Builder()
                .databaseName("test")
                .serviceName("test")
                .dbType("oracle")
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