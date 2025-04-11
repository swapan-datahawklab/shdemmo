package com.example.shelldemo.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(SqlScriptParser.class);
    private static final String STATEMENT_DELIMITER = ";";
    private static final String SINGLE_LINE_COMMENT = "--";
    private static final String MULTI_LINE_COMMENT_START = "/*";
    private static final String MULTI_LINE_COMMENT_END = "*/";
    private static final Pattern WHITESPACE = Pattern.compile("^\\s*$");

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
        logger.debug("Starting to parse SQL script file: {}", scriptFile.getPath());
        validateFile(scriptFile);
        
        List<String> statements = new ArrayList<>();
        ParseState state = new ParseState();

        List<String> lines = Files.readAllLines(scriptFile.toPath());
        logger.debug("Read {} lines from script file", lines.size());
        
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
        logger.trace("Validating script file");
        if (scriptFile == null) {
            String errorMessage = "Script file cannot be null";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        if (!scriptFile.exists()) {
            String errorMessage = "Script file does not exist: " + scriptFile.getPath();
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        if (!scriptFile.isFile()) {
            String errorMessage = "Path is not a file: " + scriptFile.getPath();
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        logger.trace("Script file validation successful");
    }

    private static void processLine(String line, ParseState state, List<String> statements) {
        if (WHITESPACE.matcher(line).matches()) {
            logger.trace("Skipping whitespace line");
            return;
        }

        String processedLine = line.trim();
        logger.trace("Processing line: {}", processedLine);
        
        // Handle single-line comments
        if (processedLine.startsWith(SINGLE_LINE_COMMENT)) {
            logger.trace("Skipping single-line comment");
            return;
        }

        // Handle multi-line comments
        if (state.inMultilineComment) {
            logger.trace("In multi-line comment, looking for end marker");
            int endIndex = processedLine.indexOf(MULTI_LINE_COMMENT_END);
            if (endIndex == -1) {
                logger.trace("No end marker found, skipping line");
                return;
            }
            processedLine = processedLine.substring(endIndex + MULTI_LINE_COMMENT_END.length()).trim();
            state.inMultilineComment = false;
            logger.trace("Found end marker, remaining line: {}", processedLine);
        }

        int startIndex = processedLine.indexOf(MULTI_LINE_COMMENT_START);
        while (startIndex != -1) {
            logger.trace("Found multi-line comment start at position {}", startIndex);
            int endIndex = processedLine.indexOf(MULTI_LINE_COMMENT_END, startIndex);
            if (endIndex == -1) {
                state.inMultilineComment = true;
                processedLine = processedLine.substring(0, startIndex).trim();
                logger.trace("No end marker found, truncating line to: {}", processedLine);
                break;
            }
            // Remove the comment and continue checking for more comments
            processedLine = (processedLine.substring(0, startIndex) + " " + 
                           processedLine.substring(endIndex + MULTI_LINE_COMMENT_END.length())).trim();
            logger.trace("Removed comment, line is now: {}", processedLine);
            startIndex = processedLine.indexOf(MULTI_LINE_COMMENT_START);
        }

        if (!processedLine.isEmpty()) {
            state.currentStatement.append(processedLine).append(" ");
            logger.trace("Added line to current statement");
            if (processedLine.endsWith(STATEMENT_DELIMITER)) {
                logger.debug("Found statement delimiter, adding statement to list");
                addStatement(state, statements);
            }
        }
    }

    private static void addStatement(ParseState state, List<String> statements) {
        String sql = state.currentStatement.toString().trim();
        // Remove the trailing delimiter
        sql = sql.substring(0, sql.length() - STATEMENT_DELIMITER.length()).trim();
        if (!sql.isEmpty()) {
            logger.debug("Adding SQL statement: {}", sql);
            statements.add(sql);
        }
        state.currentStatement.setLength(0);
        logger.trace("Reset current statement buffer");
    }

    private static void addFinalStatement(ParseState state, List<String> statements) {
        String finalSql = state.currentStatement.toString().trim();
        if (!finalSql.isEmpty()) {
            logger.debug("Processing final statement");
            if (finalSql.endsWith(STATEMENT_DELIMITER)) {
                finalSql = finalSql.substring(0, finalSql.length() - STATEMENT_DELIMITER.length());
            }
            logger.debug("Adding final SQL statement: {}", finalSql);
            statements.add(finalSql);
        }
    }

    private static class ParseState {
        private final StringBuilder currentStatement = new StringBuilder();
        private boolean inMultilineComment = false;
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