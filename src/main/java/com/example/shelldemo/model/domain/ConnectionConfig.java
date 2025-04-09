package com.example.shelldemo.model.domain;

/**
 * Base configuration class for database connections.
 * Holds common parameters for establishing a database connection.
 */
public class ConnectionConfig {
    private String host;
    private int port;
    private String serviceName;
    private String username;
    private String password;
    private boolean useSsl;
    private int connectionTimeout = 5000;
    private int readTimeout = 5000;
    private String databaseType; // "oracle" or "ldap"
    private String dbType;
    
    public ConnectionConfig() {
        this.databaseType = "oracle"; // Default to oracle
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
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getConnectionUrl() {
        return String.format("jdbc:oracle:thin:@%s:%d/%s", host, port, serviceName);
    }

    public boolean isLdap() {
        return "ldap".equalsIgnoreCase(databaseType);
    }

    public boolean isOracle() {
        return "oracle".equalsIgnoreCase(databaseType);
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
} 


