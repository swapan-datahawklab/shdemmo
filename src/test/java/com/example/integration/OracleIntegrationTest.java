package com.example.integration;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SuppressWarnings("resource")
public class OracleIntegrationTest {
    @Container
    private static final OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23.4-slim-faststart")
            .withDatabaseName("testdb")
            .withUsername(" hr")
            .withPassword("hr");

    @Test
    void testDatabaseConnection() throws SQLException {
        String jdbcUrl = oracle.getJdbcUrl();
        String username = oracle.getUsername();
        String password = oracle.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement preparedStatement = conn.prepareStatement("SELECT 1 FROM DUAL")) {
            assertTrue(conn.isValid(5));
            // simple query to verify connection and database functionality
            try (ResultSet rs = preparedStatement.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    void testDataInsertionAndRetrieval() throws Exception {
        String jdbcUrl = oracle.getJdbcUrl();
        String username = oracle.getUsername();
        String password = oracle.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            // Create a table and insert data
            try (PreparedStatement createTableStmt = conn.prepareStatement(
                    "CREATE TABLE test_data (id NUMBER, name VARCHAR2(100))")) {
                createTableStmt.execute();
            }
            try (PreparedStatement insertStmt = conn.prepareStatement(
                    "INSERT INTO test_data (id, name) VALUES (?, ?)")) {
                insertStmt.setInt(1, 1);
                insertStmt.setString(2, "Test Value");
                insertStmt.execute();
            }
            try (PreparedStatement selectStmt = conn.prepareStatement("SELECT name FROM test_data WHERE id = ?")) {
                selectStmt.setInt(1, 1);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("Test Value", rs.getString("name"));
                }
            }
        }
    }

    @Test
    void testInitScript() throws Exception {
        MountableFile initScript = MountableFile.forClasspathResource("init.sql");

        oracle.copyFileToContainer(initScript, "/tmp/init.sql");
        oracle.execInContainer("sqlplus", "-S", "test/test@//localhost:" + oracle.getOraclePort() + "/" + oracle.getSid(), "@tmp/init.sql");

        String jdbcUrl = oracle.getJdbcUrl();
        String username = oracle.getUsername();
        String password = oracle.getPassword();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM test_table WHERE id = ?")) {
            stmt.setInt(1, 1);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("test1", rs.getString("name"));
            }
        }
    }
}