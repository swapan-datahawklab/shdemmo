package com.example.shelldemo.parser.storedproc;

public enum ParamType {
    IN("IN"),
    OUT("OUT"),
    INOUT("IN/OUT");

    private final String description;

    ParamType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
} 