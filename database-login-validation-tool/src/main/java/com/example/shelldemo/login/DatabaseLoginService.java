package com.example.shelldemo.login;

import com.example.shelldemo.UnifiedDatabaseOperation;
import com.example.shelldemo.UnifiedDatabaseOperationBuilder;
import com.example.shelldemo.exception.DatabaseException;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for handling database login operations.
 */
public class DatabaseLoginService {
    private static final Logger logger = LogManager.getLogger(DatabaseLoginService.class);
    private static final int DEFAULT_THREAD_COUNT = 10;
    private final ExecutorService executorService;
    private final int threadCount;

    private static final Map<String, String> TEST_QUERIES = Map.of(
        "oracle", "SELECT 1 FROM DUAL",
        "postgresql", "SELECT 1",
        "mysql", "SELECT 1",
        "sqlserver", "SELECT 1"
    );

    public DatabaseLoginService() {
        this(DEFAULT_THREAD_COUNT);
    }

    public DatabaseLoginService(int threadCount) {
        this.threadCount = threadCount;
        this.executorService = Executors.newFixedThreadPool(threadCount);
        logger.info("Initialized DatabaseLoginService with {} threads", threadCount);
    }

    public void runTests(String outputFile) throws IOException {
        logger.info("Starting database tests with {} virtual threads", threadCount);
        
        // Create virtual thread executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            DatabaseLoginServiceTestResult result = executor.submit(this::testDatabase).get();
            List<DatabaseLoginServiceTestResult> results = List.of(result);
            writeResultsToCsv(results, outputFile);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Database test interrupted", e);
            throw new IOException("Database test interrupted", e);
        } catch (Exception e) {
            logger.error("Error running database tests", e);
            throw new IOException("Failed to run database tests", e);
        }
    }

    private String getTestQuery(String dbType) {
        return TEST_QUERIES.getOrDefault(dbType, "SELECT 1");
    }

    private DatabaseLoginServiceTestResult testDatabase() {
        long startTime = System.currentTimeMillis();
        
        try (UnifiedDatabaseOperation operation = new UnifiedDatabaseOperationBuilder()
                .dbType("oracle")
                .host("localhost")
                .port(1521)
                .username("user")
                .password("pass")
                .build()) {
            operation.executeQuery(getTestQuery("oracle")); // Execute query without storing results
            return buildResult(true, startTime, null);
        } catch (Exception e) {
            logger.error("Error testing database: {}", e.getMessage());
            String sqlState = (e instanceof DatabaseException && e.getCause() instanceof SQLException) 
                ? ((SQLException) e.getCause()).getSQLState() 
                : "ERROR";
            return buildResult(false, startTime, new ErrorInfo(e.getMessage(), sqlState));
        }
    }

    private void writeResultsToCsv(List<DatabaseLoginServiceTestResult> results, String outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(DatabaseLoginServiceTestResult.getCsvHeader() + "\n");
            for (DatabaseLoginServiceTestResult result : results) {
                writer.write(result.toCsvRecord() + "\n");
            }
        }
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        logger.info("Shutting down DatabaseLoginService");
        executorService.shutdown();
    }

    private record ErrorInfo(String message, String sqlState) {}

    private DatabaseLoginServiceTestResult buildResult(boolean success, long startTime, ErrorInfo error) {
        DatabaseLoginServiceTestResult.Builder builder = new DatabaseLoginServiceTestResult.Builder()
            .databaseName("test")
            .serviceName("test")
            .dbType("oracle")
            .success(success)
            .responseTimeMs(System.currentTimeMillis() - startTime);
        
        if (error != null) {
            builder.error(error.message(), error.sqlState());
        }
        
        return builder.build();
    }
} 