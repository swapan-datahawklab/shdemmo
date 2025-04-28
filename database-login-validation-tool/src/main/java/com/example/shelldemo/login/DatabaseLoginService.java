package com.example.shelldemo.login;

import com.example.shelldemo.login.exception.DatabaseConnectionException;
import com.example.shelldemo.login.exception.DatabaseOperationException;
import com.example.shelldemo.UnifiedDatabaseOperation;
import com.example.shelldemo.connection.ConnectionConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
        
        try {
            // Run database test using UnifiedDatabaseOperation
            ConnectionConfig config = new ConnectionConfig();
            config.setHost("localhost");
            config.setServiceName("test");
            config.setUsername("test");
            config.setPassword("test");
            
            try (UnifiedDatabaseOperation operation = UnifiedDatabaseOperation.create("oracle", config)) {
                List<Map<String, Object>> results = operation.executeQuery(getTestQuery("oracle"));
                long responseTime = System.currentTimeMillis() - startTime;
                
                return new DatabaseLoginServiceTestResult.Builder()
                    .databaseName("test")
                    .serviceName("test")
                    .dbType("oracle")
                    .success(!results.isEmpty())
                    .responseTimeMs(responseTime)
                    .build();
            } catch (DatabaseOperationException | DatabaseConnectionException e) {
                long responseTime = System.currentTimeMillis() - startTime;
                logger.error("Error testing database: {}", e.getMessage());
                
                return new DatabaseLoginServiceTestResult.Builder()
                    .databaseName("test")
                    .serviceName("test")
                    .dbType("oracle")
                    .success(false)
                    .responseTimeMs(responseTime)
                    .error(e.getMessage(), e.getCause() instanceof SQLException sqlException ? sqlException.getSQLState() : "ERROR")
                    .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error testing database: {}", e.getMessage());
            
            return new DatabaseLoginServiceTestResult.Builder()
                .databaseName("test")
                .serviceName("test")
                .dbType("oracle")
                .success(false)
                .responseTimeMs(responseTime)
                .error(e.getMessage(), "ERROR")
                .build();
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
     * Attempts to establish a database connection using the provided credentials.
     *
     * @param url The database URL
     * @param username The database username
     * @param password The database password
     * @return A Connection object if successful
     * @throws SQLException If the connection attempt fails
     */
    public Connection login(String url, String username, String password) throws SQLException {
        logger.debug("Attempting database login for URL: {}, username: {}", url, username);
        
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        
        try {
            Connection conn = DriverManager.getConnection(url, props);
            logger.info("Successfully connected to database: {}", url);
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to connect to database: {}", url, e);
            throw e;
        }
    }

    /**
     * Validates the login credentials without establishing a connection.
     *
     * @param url The database URL
     * @param username The database username
     * @param password The database password
     * @return true if the credentials are valid, false otherwise
     */
    public boolean validateCredentials(String url, String username, String password) {
        logger.debug("Validating credentials for URL: {}, username: {}", url, username);
        
        if (url == null || url.trim().isEmpty()) {
            logger.error("Database URL cannot be null or empty");
            return false;
        }
        
        if (username == null || username.trim().isEmpty()) {
            logger.error("Username cannot be null or empty");
            return false;
        }
        
        if (password == null || password.trim().isEmpty()) {
            logger.error("Password cannot be null or empty");
            return false;
        }
        
        logger.trace("Credentials validated successfully");
        return true;
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        logger.info("Shutting down DatabaseLoginService");
        executorService.shutdown();
    }
} 