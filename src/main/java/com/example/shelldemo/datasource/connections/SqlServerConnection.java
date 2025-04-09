package com.example.shelldemo.datasource.connections;

import com.example.shelldemo.model.base.AbstractDatabaseConnection;
import com.example.shelldemo.model.domain.ConnectionConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlServerConnection extends AbstractDatabaseConnection {
    public SqlServerConnection(ConnectionConfig config) {
        super(config);
    }

    @Override
    protected Connection createConnection() throws SQLException {
        return createConnection(config);
    }

    @Override
    protected Connection createConnection(ConnectionConfig config) throws SQLException {
        String url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s",
            config.getHost(),
            config.getPort(),
            config.getServiceName());
        return DriverManager.getConnection(url, config.getUsername(), config.getPassword());
    }
} 