package com.example.shelldemo.validate;

import com.example.shelldemo.parser.storedproc.ParamType;
import com.example.shelldemo.exception.StoredProcedureParseException;

public class StoredProcedureValidator {
    public static void validateParts(String[] parts, ParamType type, int paramIndex) {
        if (parts.length < 2) {
            throw new StoredProcedureParseException(
                String.format(
                    "Invalid parameter format at position %d. Expected 'name:type[:value]', got '%s'",
                    paramIndex,
                    String.join(":", parts)
                ),
                StoredProcedureParseException.ERROR_CODE_INVALID_FORMAT
            );
        }

        if ((type == ParamType.IN || type == ParamType.INOUT) && parts.length < 3) {
            throw new StoredProcedureParseException(
                String.format(
                    "Missing value for %s parameter at position %d",
                    type,
                    paramIndex
                ),
                StoredProcedureParseException.ERROR_CODE_MISSING_VALUE
            );
        }

        if (type == ParamType.OUT && parts.length > 2) {
            throw new StoredProcedureParseException(
                String.format(
                    "OUT parameter at position %d should not have a value",
                    paramIndex
                ),
                StoredProcedureParseException.ERROR_CODE_INVALID_PARAM
            );
        }
    }

    public static void validateDefinition(String definition) {
        if (definition == null || definition.trim().isEmpty()) {
            throw new StoredProcedureParseException(
                "Stored procedure definition cannot be null or empty",
                StoredProcedureParseException.ERROR_CODE_INVALID_FORMAT
            );
        }
    }
} 