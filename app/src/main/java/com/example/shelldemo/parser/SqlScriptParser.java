package com.example.shelldemo.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

import com.example.shelldemo.exception.SqlParseException;

/**
 * Utility class for parsing SQL script files.
 * Handles comment removal and statement separation.
 */
public final class SqlScriptParser {
    private static final Logger logger = LogManager.getLogger(SqlScriptParser.class);

    private SqlScriptParser() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Parses a SQL script file into a map of individual SQL statements.
     *
     * @param scriptFile the SQL script file to parse
     * @return Map of statement numbers to SQL statements
     * @throws SqlParseException if parsing fails
     */
    public static Map<Integer, String> parseSqlFile(File scriptFile) throws SqlParseException {
        SqlParseException.validateScriptFile(scriptFile);
        
        logger.debug("Starting SQL file parsing: {}", scriptFile.getName());

        try {
            String content = new String(Files.readAllBytes(scriptFile.toPath()));
            String processedContent = removeComments(content);
            return parseStatements(processedContent);
        } catch (IOException e) {
            throw new SqlParseException(
                "Failed to parse SQL file: " + scriptFile.getName(), 
                e, SqlParseException.ERROR_CODE_FILE_IO_ERROR
            );
        } catch (IllegalArgumentException e) {
            throw new SqlParseException(
                e.getMessage(), e, SqlParseException.ERROR_CODE_INVALID_FORMAT
            );
        }
    }

    private static ProcessResult processChar(char c, char next, CommentParserState state) {
        // Check for line endings in line comments
        if (isLineEnd(c) && state.inLineComment) {
            state.inLineComment = false;
            return ProcessResult.append();
        }

        // Skip chars in comments
        if (state.inAnyComment()) {
            if (state.inMultiLineComment && c == '*' && next == '/') {
                state.inMultiLineComment = false;
                return ProcessResult.skip(1);
            }
            return ProcessResult.skip(0);
            }
        
        // Handle string delimiters
        if (isStringDelimiter(c, state)) {
            processStringDelimiter(c, state);
            return ProcessResult.append();
        }

        // Check for comment starts (outside strings)
        if (!state.inAnyString()) {
            if (c == '-' && next == '-') {
                state.inLineComment = true;
                return ProcessResult.skip(1);
            }
            
            if (c == '/' && next == '*') {
                state.inMultiLineComment = true;
                return ProcessResult.skip(1);
            }
        }

        // Default: append character
        return ProcessResult.append();
    }

    private static String removeComments(String content) {
        StringBuilder processed = new StringBuilder();
        CommentParserState state = new CommentParserState();
        int i = 0;
        
        while (i < content.length()) {
            char c = content.charAt(i);
            char next = (i < content.length() - 1) ? content.charAt(i + 1) : '\0';
            
            ProcessResult result = processChar(c, next, state);
            if (result.appendChar) {
                processed.append(c);
            }
            
            i += 1 + result.indexShift;
        }
        
        return processed.toString();
    }
    
    private static boolean isLineEnd(char c) {
        return c == '\n' || c == '\r';
    }

    private static boolean isStringDelimiter(char c, CommentParserState state) {
        return (c == '\'' && !state.inDoubleQuote) || (c == '"' && !state.inSingleQuote);
    }

    private static void processStringDelimiter(char c, CommentParserState state) {
        if (c == '\'') {
            state.inSingleQuote = !state.inSingleQuote;
        } else if (c == '"') {
            state.inDoubleQuote = !state.inDoubleQuote;
        }
    }

    private static Map<Integer, String> parseStatements(String content) {
        Map<Integer, String> statements = new HashMap<>();
        String[] lines = content.replace("\r\n", "\n").split("\n");
        StatementParser parser = new StatementParser(statements);
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            parser.processLine(line);
        }
        
        parser.addRemainingStatement();
        return statements;
    }
}