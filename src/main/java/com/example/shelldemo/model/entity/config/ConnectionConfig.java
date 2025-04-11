package com.example.shelldemo.model.entity.config;

/**
 * Base configuration class for database connections.
 * Contains common properties needed for establishing database connections.
 */
public class ConnectionConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String serviceName;
    private String dbType;
    private String connectionType;  // For Oracle: "thin" or "ldap"

    public ConnectionConfig() {
        // Default constructor
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String getConnectionUrl() {
        return String.format("jdbc:%s://%s:%d/%s", 
            getDbType(), getHost(), getPort(), getServiceName());
    }
} 