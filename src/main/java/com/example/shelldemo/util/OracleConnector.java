package com.example.shelldemo.util;

import com.example.shelldemo.model.ConnectionConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OracleConnector {
    private final ConnectionConfig config;

    public OracleConnector(ConnectionConfig config) {
        this.config = config;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            config.getConnectionUrl(),
            config.getUsername(),
            config.getPassword()
        );
    }
} 