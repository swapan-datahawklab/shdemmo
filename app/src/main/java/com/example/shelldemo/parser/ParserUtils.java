package com.example.shelldemo.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Shared utility methods for parsing operations.
 */
public final class ParserUtils {
    private static final Logger logger = LogManager.getLogger(ParserUtils.class);

    private ParserUtils() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Validates that a string is not null or empty after trimming.
     *
     * @param value the string to validate
     * @param fieldName name of the field being validated
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            String errorMessage = fieldName + " cannot be null or empty";
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Base exception class for parsing errors.
     */
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
            logger.error(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
            logger.error(message, cause);
        }
    }
} 