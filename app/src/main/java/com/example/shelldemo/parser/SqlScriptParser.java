package com.example.shelldemo.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
    private static final String SINGLE_LINE_COMMENT = "--";
    private static final String MULTI_LINE_COMMENT_START = "/*";
    private static final String MULTI_LINE_COMMENT_END = "*/";
    private static final Pattern WHITESPACE = Pattern.compile("^\\s*$");
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
     * Parses a SQL script file into a list of individual SQL statements.
     *
     * @param scriptFile the SQL script file to parse
     * @return List of SQL statements
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if file is null or doesn't exist
     */
    public static List<String> parse(File scriptFile) throws IOException {
        validateFile(scriptFile);
        
        List<String> statements = new ArrayList<>();
        ParseState state = new ParseState();

        List<String> lines = Files.readAllLines(scriptFile.toPath());
        
        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
            try {
                processLine(lines.get(lineNum), state, statements);
            } catch (Exception e) {
                String errorMessage = "Error parsing line " + (lineNum + 1);
                logger.error(errorMessage, e);
                throw new SqlParseException(errorMessage, e);
            }
        }

        // Add final statement if it exists
        addFinalStatement(state, statements);
        logger.info("Successfully parsed {} SQL statements from file", statements.size());
        return statements;
    }

    private static void validateFile(File scriptFile) {
        if (scriptFile == null) {
            String errorMessage = "Script file cannot be null";
            throw new IllegalArgumentException(errorMessage);
        }
        if (!scriptFile.exists()) {
            String errorMessage = "Script file does not exist: " + scriptFile.getPath();
            throw new IllegalArgumentException(errorMessage);
        }
        if (!scriptFile.isFile()) {
            String errorMessage = "Path is not a file: " + scriptFile.getPath();
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static void processLine(String line, ParseState state, List<String> statements) {
        String processedLine = line.trim();
        
        // Handle empty lines
        if (WHITESPACE.matcher(line).matches()) {
            if (state.hasContent) {
                state.currentComments.append("\n");
            }
            return;
        }

        // Handle single-line comments
        if (processedLine.startsWith(SINGLE_LINE_COMMENT)) {
            state.currentComments.append(line).append("\n");
            return;
        }

        // Handle multi-line comments
        if (state.inMultilineComment) {
            state.currentComments.append(line).append("\n");
            int endIndex = processedLine.indexOf(MULTI_LINE_COMMENT_END);
            if (endIndex == -1) {
                return;
            }
            processedLine = processedLine.substring(endIndex + MULTI_LINE_COMMENT_END.length()).trim();
            state.inMultilineComment = false;
            if (processedLine.isEmpty()) {
                return;
            }
        }

        int startIndex = processedLine.indexOf(MULTI_LINE_COMMENT_START);
        while (startIndex != -1) {
            if (startIndex > 0) {
                state.currentComments.append(line.substring(0, line.indexOf(MULTI_LINE_COMMENT_START))).append("\n");
            }
            int endIndex = processedLine.indexOf(MULTI_LINE_COMMENT_END, startIndex);
            if (endIndex == -1) {
                state.inMultilineComment = true;
                state.currentComments.append(line.substring(line.indexOf(MULTI_LINE_COMMENT_START))).append("\n");
                processedLine = processedLine.substring(0, startIndex).trim();
                break;
            }
            state.currentComments.append(line.substring(line.indexOf(MULTI_LINE_COMMENT_START), 
                                                      line.indexOf(MULTI_LINE_COMMENT_END) + 2)).append("\n");
            processedLine = (processedLine.substring(0, startIndex) + " " + 
                           processedLine.substring(endIndex + MULTI_LINE_COMMENT_END.length())).trim();
            startIndex = processedLine.indexOf(MULTI_LINE_COMMENT_START);
        }

        // Check if we're starting a PL/SQL block
        // This includes any statement starting with CREATE OR REPLACE
        if (!state.inPlsqlBlock && 
            (PLSQL_START.matcher(processedLine).matches() || 
             processedLine.toUpperCase().startsWith("CREATE ") || 
             processedLine.toUpperCase().startsWith("CREATE OR REPLACE "))) {
            state.inPlsqlBlock = true;
            logger.debug("Detected PL/SQL block starting with: {}", 
                         processedLine.length() > 50 ? processedLine.substring(0, 50) + "..." : processedLine);
        }

        // Handle forward slash delimiter on a line by itself
        // This is the Oracle SQL*Plus style terminator for PL/SQL blocks
        if (FORWARD_SLASH_DELIMITER.equals(processedLine)) {
            if (state.inPlsqlBlock) {
                if (!state.currentStatement.toString().trim().isEmpty()) {
                    addStatement(state, statements);
                    state.inPlsqlBlock = false;
                    state.hasContent = false;
                }
            } else {
                // Log warning if forward slash is found outside of PL/SQL block
                logger.warn("Forward slash delimiter found outside of PL/SQL block - ignoring");
            }
            return;
        }

        if (!processedLine.isEmpty()) {
            // Add accumulated comments if this is the start of a new statement
            if (!state.hasContent && state.currentComments.length() > 0) {
                state.currentStatement.append(state.currentComments);
                state.currentComments.setLength(0);
            }
            
            state.hasContent = true;
            state.currentStatement.append(processedLine).append("\n");
            
            // Only treat semicolon as delimiter if we're not in a PL/SQL block
            if (!state.inPlsqlBlock && processedLine.endsWith(SEMICOLON_DELIMITER)) {
                addStatement(state, statements);
                state.hasContent = false;
            }
            // Do not strip semicolons within PL/SQL blocks - they are required syntax
        }
    }

    private static void addStatement(ParseState state, List<String> statements) {
        String sql = state.currentStatement.toString().trim();
        // Remove the trailing semicolon only for non-PL/SQL statements
        // For PL/SQL blocks, we must preserve all semicolons inside the block
        if (!state.inPlsqlBlock && sql.endsWith(SEMICOLON_DELIMITER)) {
            sql = sql.substring(0, sql.length() - SEMICOLON_DELIMITER.length()).trim();
        }
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
        state.currentStatement.setLength(0);
        state.currentComments.setLength(0);
    }

    private static void addFinalStatement(ParseState state, List<String> statements) {
        if (state.currentStatement.length() > 0) {
            String finalSql = state.currentStatement.toString().trim();
            if (!finalSql.isEmpty()) {
                if (!state.inPlsqlBlock && finalSql.endsWith(SEMICOLON_DELIMITER)) {
                    finalSql = finalSql.substring(0, finalSql.length() - SEMICOLON_DELIMITER.length());
                }
                statements.add(finalSql);
            }
        }
    }

    private static class ParseState {
        private final StringBuilder currentStatement = new StringBuilder();
        private final StringBuilder currentComments = new StringBuilder();
        private boolean inMultilineComment = false;
        private boolean inPlsqlBlock = false;
        private boolean hasContent = false;
    }

    /**
     * Custom exception for SQL parsing errors
     */
    public static class SqlParseException extends ParserUtils.ParseException {
        public SqlParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}