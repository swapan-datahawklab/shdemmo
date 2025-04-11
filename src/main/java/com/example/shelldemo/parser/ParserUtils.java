package com.example.shelldemo.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utility methods for parsing operations.
 */
public final class ParserUtils {
    private static final Logger logger = LoggerFactory.getLogger(ParserUtils.class);

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
        logger.trace("Validating field: {}", fieldName);
        if (value == null || value.trim().isEmpty()) {
            String errorMessage = fieldName + " cannot be null or empty";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        logger.trace("Field {} validation successful", fieldName);
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