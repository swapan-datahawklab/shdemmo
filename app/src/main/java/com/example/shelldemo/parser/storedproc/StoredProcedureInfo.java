package com.example.shelldemo.parser.storedproc;

public class StoredProcedureInfo {
    private final String name;
    private final String parameters;

    public StoredProcedureInfo(String name, String parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "StoredProcedureInfo{" +
               "name='" + name + '\'' +
               ", parameters='" + parameters + '\'' +
               '}';
    }
} 