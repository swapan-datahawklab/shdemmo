package com.example.shelldemo.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

/**
 * Parses SQL script files into individual SQL statements.
 * Handles:
 * - Single-line comments (--)
 * - Multi-line comments (/* ... *\/)
 * - Statement delimiters (;)
 * - Quoted strings (to avoid parsing comments or delimiters within strings)
 */
public final class SqlScriptParser {
    private static final Logger logger = LogManager.getLogger(SqlScriptParser.class);
    private static final String SEMICOLON_DELIMITER = ";";
    private static final String FORWARD_SLASH_DELIMITER = "/";
    // private static final String SINGLE_LINE_COMMENT = "--";
    // private static final String MULTI_LINE_COMMENT_START = "/*";
    // private static final String MULTI_LINE_COMMENT_END = "*/";
    // private static final Pattern WHITESPACE = Pattern.compile("^\\s*$");
    private static final Pattern PLSQL_START = Pattern.compile("^(BEGIN|DECLARE|CREATE\\s+(OR\\s+REPLACE\\s+)?(" +
        "FUNCTION|PROCEDURE|TRIGGER|PACKAGE|PACKAGE\\s+BODY|" +
        "TYPE|TYPE\\s+BODY|VIEW|MATERIALIZED\\s+VIEW|" +
        "LIBRARY|JAVA|CONTEXT|DIRECTORY|SYNONYM|EDITION|" +
        "DATABASE\\s+TRIGGER|INSTEAD\\s+OF\\s+TRIGGER))\\s+.*", 
        Pattern.CASE_INSENSITIVE);

    private SqlScriptParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Parses a SQL script file into a map of individual SQL statements.
     *
     * @param scriptFile the SQL script file to parse
     * @return Map of statement numbers to SQL statements
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if file is null or doesn't exist
     */
    public static Map<Integer, String> parseSqlFile(File scriptFile) throws IOException {
        validateFile(scriptFile);
        
        Map<Integer, String> statements = new HashMap<>();
        ParseState state = new ParseState();
        int[] statementCount = {0}; // Use an array to allow modification

        List<String> lines = Files.readAllLines(scriptFile.toPath());
        
        for (String line : lines) {
            String processedLine = stripComments(line).trim(); // Strip comments and trim
            
            // Skip empty lines
            if (processedLine.isEmpty()) {
                continue;
            }

            // Call processLine to handle the logic for each line
            processLine(processedLine, state, statements, statementCount);
        }

        // Add any remaining statement if it exists
        addFinalStatement(state, statements);

        logger.info("Successfully parsed {} SQL statements from file", statements.size());
        return statements;
    }

    private static String stripComments(String line) {
        StringBuilder result = new StringBuilder();
        String[] lines = line.split("\\R"); // Split by any line break
    
        for (String singleLine : lines) {
            String trimmed = singleLine.trim();
    
            // Stop when we reach a single slash "/"
            if (trimmed.equals("/")) {
                break;
            }
    
            // Skip full-line comments
            if (trimmed.startsWith("--")) {
                continue;
            }
    
            // Append the line to the result if it's not a comment
            result.append(singleLine).append(System.lineSeparator());
        }
    
        return result.toString().trim(); // Return the result without leading/trailing whitespace
    }

    private static boolean isPLSQLStatement(String line) {
        return PLSQL_START.matcher(line).find();
    }

    private static void validateFile(File scriptFile) {
        if (scriptFile == null) {
            throw new IllegalArgumentException("Script file cannot be null");
        }
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Script file does not exist: " + scriptFile.getPath());
        }
        if (!scriptFile.isFile()) {
            throw new IllegalArgumentException("Path is not a file: " + scriptFile.getPath());
        }
    }

    private static void processLine(String line, ParseState state, Map<Integer, String> statements, int[] statementCount) {
        String processedLine = line.trim();
        
        // Handle empty lines
        if (processedLine.isEmpty()) {
            return;
        }

        // Check for PL/SQL block termination
        if (processedLine.equals(FORWARD_SLASH_DELIMITER)) {
            // Finalize the current PL/SQL statement
            addStatement(state, statements, statementCount);
            state.inPlsqlBlock = false; // Reset the state after finalizing the PL/SQL block
            return; // Exit the method after processing the termination
        }

        // Check if the line is a PL/SQL statement
        if (isPLSQLStatement(processedLine)) {
            state.inPlsqlBlock = true; // Set the state to indicate we're in a PL/SQL block
        }

        // Append the line to the current statement
        state.currentStatement.append(processedLine).append("\n");

        // Check for statement termination
        if (processedLine.endsWith(SEMICOLON_DELIMITER)) {
            // Finalize the current SQL statement
            addStatement(state, statements, statementCount);
        }
    }

    private static void addStatement(ParseState state, Map<Integer, String> statements, int[] statementCount) {
        String sql = state.currentStatement.toString().trim();
        if (!sql.isEmpty()) {
            // Optionally handle PL/SQL blocks differently if needed
            if (state.inPlsqlBlock) {
                // Handle PL/SQL specific logic if necessary
            }
            statements.put(++statementCount[0], sql); // Add the complete statement with a unique key
        }
        state.currentStatement.setLength(0); // Reset for the next statement
    }

    private static void addFinalStatement(ParseState state, Map<Integer, String> statements) {
        if (state.currentStatement.length() > 0) {
            String finalSql = state.currentStatement.toString().trim();
            if (!finalSql.isEmpty()) {
                statements.put(statements.size() + 1, finalSql); // Add the final statement with a unique key
            }
        }
    }

    private static class ParseState {
        private final StringBuilder currentStatement = new StringBuilder();
        private boolean inPlsqlBlock = false;
    }

    /**
     * Custom exception for SQL parsing errors
     */
    public static class SqlParseException extends Exception {
        public SqlParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}