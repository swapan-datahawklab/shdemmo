package com.example.shelldemo;

public enum DatabaseOperationType {
    QUERY("query execution"),
    UPDATE("update operation"),
    SCRIPT_EXECUTION("script execution"),
    PROCEDURE_CALL("stored procedure call"),
    VALIDATION("validation"),
    CONNECTION("connection");

    private final String description;

    DatabaseOperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
