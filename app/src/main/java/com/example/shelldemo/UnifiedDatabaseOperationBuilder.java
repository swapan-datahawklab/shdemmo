package com.example.shelldemo;

import com.example.shelldemo.sqlhandling.DatabaseConfig;
public class UnifiedDatabaseOperationBuilder {
    private String host;
    private int port;
    private String username;
    private String password;
    private String dbType;
    private String connectionType;
    private String serviceName;

    public UnifiedDatabaseOperationBuilder host(String host) { this.host = host; return this; }
    public UnifiedDatabaseOperationBuilder port(int port) { this.port = port; return this; }
    public UnifiedDatabaseOperationBuilder username(String username) { this.username = username; return this; }
    public UnifiedDatabaseOperationBuilder password(String password) { this.password = password; return this; }
    public UnifiedDatabaseOperationBuilder dbType(String dbType) { this.dbType = dbType; return this; }
    public UnifiedDatabaseOperationBuilder connectionType(String connectionType) { this.connectionType = connectionType; return this; }
    public UnifiedDatabaseOperationBuilder serviceName(String serviceName) { this.serviceName = serviceName; return this; }

    public UnifiedDatabaseOperation build() {
        DatabaseConfig config = new DatabaseConfig();
        config.setDbType(dbType);
        config.setHost(host);
        config.setPort(port);
        config.setUsername(username);
        config.setPassword(password);
        config.setDatabase(serviceName);
        config.setConnectionType(connectionType);
        return UnifiedDatabaseOperation.create(config);
    }
}