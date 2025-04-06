package com.example.oracle.datasource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class DatabaseOperation {
    protected final DataSource dataSource;

    protected DatabaseOperation(String host, String username, String password) {
        this.dataSource = OracleDataSourceFactory.createDataSource(host, username, password);
    }

    protected <T> T execute(ConnectionCallback<T> callback) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return callback.execute(connection);
        }
    }

    @FunctionalInterface
    protected interface ConnectionCallback<T> {
        T execute(Connection connection) throws SQLException;
    }
} 