package com.example.shelldemo.model.domain;

public class DatabaseResult {
    private final String databaseName;
    private final String serviceName;
    private final boolean success;

    public DatabaseResult(String databaseName, String serviceName, boolean success) {
        this.databaseName = databaseName;
        this.serviceName = serviceName;
        this.success = success;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public boolean isSuccess() {
        return success;
    }
} 