package com.example.shelldemo.parser.storedproc;

import java.util.Objects;

public final class ProcedureParam {
    private final String name;
    private final String type;
    private final String value;
    private final ParamType paramType;

    private ProcedureParam(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Parameter name cannot be null");
        this.type = Objects.requireNonNull(builder.type, "Parameter type cannot be null");
        this.value = builder.value; // value can be null for OUT parameters
        this.paramType = Objects.requireNonNull(builder.paramType, "Parameter direction cannot be null");
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public ParamType getParamType() {
        return paramType;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s%s", 
            paramType, name, type, 
            value != null ? " = " + value : "");
    }

    public static class Builder {
        private String name;
        private String type;
        private String value;
        private ParamType paramType;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder paramType(ParamType paramType) {
            this.paramType = paramType;
            return this;
        }

        public ProcedureParam build() {
            return new ProcedureParam(this);
        }
    }
} 