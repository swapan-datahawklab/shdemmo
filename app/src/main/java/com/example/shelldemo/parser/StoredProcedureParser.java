package com.example.shelldemo.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Parser for stored procedure parameters.
 * Handles input, output, and input/output parameters in the format: name:type[:value]
 */
public final class StoredProcedureParser {
    private static final Logger logger = LoggerFactory.getLogger(StoredProcedureParser.class);
    private static final String PARAM_DELIMITER = ",";
    private static final String FIELD_DELIMITER = ":";

    private StoredProcedureParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Represents a stored procedure parameter with its properties.
     */
    public static final class ProcedureParam {
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

        /**
         * Builder for ProcedureParam.
         */
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

    /**
     * Parameter direction types.
     */
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

    /**
     * Parses procedure parameters from string representations.
     *
     * @param inputParams parameters in format "name:type:value,..."
     * @param outputParams parameters in format "name:type,..."
     * @param ioParams parameters in format "name:type:value,..."
     * @return List of parsed procedure parameters
     * @throws ProcParseException if parameter format is invalid
     */
    public static List<ProcedureParam> parse(String inputParams, String outputParams, String ioParams) {
        logger.debug("Parsing procedure parameters - IN: {}, OUT: {}, INOUT: {}", 
            maskSensitiveParams(inputParams), outputParams, maskSensitiveParams(ioParams));
        
        List<ProcedureParam> params = new ArrayList<>();
        try {
            if (inputParams != null && !inputParams.trim().isEmpty()) {
                logger.trace("Parsing IN parameters: {}", maskSensitiveParams(inputParams));
                params.addAll(parseParamString(inputParams, ParamType.IN));
            }
            if (outputParams != null && !outputParams.trim().isEmpty()) {
                logger.trace("Parsing OUT parameters: {}", outputParams);
                params.addAll(parseParamString(outputParams, ParamType.OUT));
            }
            if (ioParams != null && !ioParams.trim().isEmpty()) {
                logger.trace("Parsing INOUT parameters: {}", maskSensitiveParams(ioParams));
                params.addAll(parseParamString(ioParams, ParamType.INOUT));
            }
            
            logger.debug("Successfully parsed {} procedure parameters", params.size());
            return Collections.unmodifiableList(params);
        } catch (Exception e) {
            String errorMessage = "Failed to parse procedure parameters";
            logger.error(errorMessage, e);
            throw new ProcParseException(errorMessage, e);
        }
    }

    private static List<ProcedureParam> parseParamString(String paramString, ParamType type) {
        logger.trace("Parsing {} parameters: {}", type, 
            type == ParamType.OUT ? paramString : maskSensitiveParams(paramString));
        
        List<ProcedureParam> params = new ArrayList<>();
        String[] paramPairs = paramString.trim().split(PARAM_DELIMITER);
        
        for (int i = 0; i < paramPairs.length; i++) {
            String pair = paramPairs[i].trim();
            if (pair.isEmpty()) {
                logger.trace("Skipping empty parameter at position {}", i + 1);
                continue;
            }

            logger.trace("Parsing parameter at position {}: {}", i + 1, 
                type == ParamType.OUT ? pair : maskSensitiveParam(pair));
            
            String[] parts = pair.split(FIELD_DELIMITER);
            validateParts(parts, type, i + 1);

            ProcedureParam param = new ProcedureParam.Builder()
                .name(parts[0].trim())
                .type(parts[1].trim())
                .value(parts.length > 2 ? parts[2].trim() : null)
                .paramType(type)
                .build();
                
            logger.trace("Created parameter: {}", maskSensitiveParam(param.toString()));
            params.add(param);
        }
        
        logger.debug("Successfully parsed {} {} parameters", params.size(), type);
        return params;
    }

    private static void validateParts(String[] parts, ParamType type, int paramIndex) {
        logger.trace("Validating parameter parts at position {}", paramIndex);
        
        if (parts.length < 2) {
            String errorMessage = String.format(
                "Invalid parameter format at position %d. Expected 'name:type[:value]', got '%s'",
                paramIndex, String.join(FIELD_DELIMITER, parts));
            logger.error(errorMessage);
            throw new ProcParseException(errorMessage);
        }

        try {
            ParserUtils.validateNotEmpty(parts[0].trim(), "Parameter name");
            ParserUtils.validateNotEmpty(parts[1].trim(), "Parameter type");
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format("%s at position %d", e.getMessage(), paramIndex);
            logger.error(errorMessage);
            throw new ProcParseException(errorMessage);
        }

        if (type != ParamType.OUT && parts.length < 3) {
            String errorMessage = String.format("Value required for %s parameter at position %d", type, paramIndex);
            logger.error(errorMessage);
            throw new ProcParseException(errorMessage);
        }
        
        logger.trace("Parameter validation successful at position {}", paramIndex);
    }

    private static String maskSensitiveParams(String params) {
        if (params == null) return null;
        String[] pairs = params.split(PARAM_DELIMITER);
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = maskSensitiveParam(pairs[i].trim());
        }
        return String.join(PARAM_DELIMITER, pairs);
    }

    private static String maskSensitiveParam(String param) {
        if (param == null || param.isEmpty()) return param;
        String[] parts = param.split(FIELD_DELIMITER);
        if (parts.length <= 2) return param;
        
        // Mask the value part if it looks like a password or sensitive data
        String value = parts[2].trim().toLowerCase();
        if (value.contains("password") || value.contains("secret") || 
            value.contains("key") || value.contains("token")) {
            parts[2] = "********";
        }
        return String.join(FIELD_DELIMITER, parts);
    }

    /**
     * Custom exception for procedure parameter parsing errors.
     */
    public static class ProcParseException extends ParserUtils.ParseException {
        public ProcParseException(String message) {
            super(message);
        }

        public ProcParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 