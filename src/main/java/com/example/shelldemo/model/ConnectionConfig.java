package com.example.shelldemo.model;

/**
 * Configuration class for database connections.
 * Holds all necessary parameters for establishing a database connection.
 */
public class ConnectionConfig {
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    
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
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
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

    public String getConnectionUrl() {
        return String.format("jdbc:oracle:thin:@%s:%d/%s", host, port, database);
    }
} 


