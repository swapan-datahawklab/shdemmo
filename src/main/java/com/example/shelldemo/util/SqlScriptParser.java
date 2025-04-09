package com.example.shelldemo.util; // Or com.example.shelldemo.parser

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a SQL script file into a list of individual SQL statements.
 * Handles single-line (--), multi-line (/* ... * /), and inline comments (--).
 * Statements are expected to be terminated by a semicolon (;).
 *
 * Note: This parser is basic and might not handle complex scenarios like
 * comments or semicolons within string literals or specific SQL dialect variations perfectly.
 */
final class SqlScriptParser { // Make final as it's a utility class

    // --- Inner class to hold parsing state ---
    private static class ParseState {
        final StringBuilder currentStatement = new StringBuilder();
        boolean inMultilineComment = false;
    }
    // --- End Inner class ---

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SqlScriptParser() {
        // Utility class
    }

    /**
     * Parses the given SQL script file into a list of statements.
     *
     * @param scriptFile The SQL script file to parse.
     * @return A list of SQL statements found in the file.
     * @throws IOException If an error occurs reading the file.
     */
    public static List<String> parse(File scriptFile) throws IOException {
        List<String> statements = new ArrayList<>();
        ParseState state = new ParseState(); // Holds currentStatement and inMultilineComment

        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line, state, statements);
            }
        }

        // Add any remaining content in the buffer as the last statement
        addFinalStatement(statements, state);

        return statements;
    }

    /**
     * Processes a single line from the SQL script file.
     * Delegates to appropriate handlers based on the current parsing state.
     */
    private static void processLine(String line, ParseState state, List<String> statements) {
        String trimmedLine = line.trim();

        if (state.inMultilineComment) {
            handleLineInMultilineComment(trimmedLine, state);
        } else {
            handleRegularLine(trimmedLine, state, statements);
        }
    }

    /**
     * Handles processing a line when currently inside a multiline comment block.
     */
    private static void handleLineInMultilineComment(String trimmedLine, ParseState state) {
        int endCommentPos = trimmedLine.indexOf("*/");
        if (endCommentPos != -1) {
            state.inMultilineComment = false;
        }
    }

    /**
     * Handles processing a line when *not* inside a multiline comment block.
     */
    private static void handleRegularLine(String trimmedLine, ParseState state, List<String> statements) {
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("--") || trimmedLine.startsWith("//")) {
            return; // Skip empty lines and single-line comments
        }

        StringBuilder lineProcessor = new StringBuilder(trimmedLine);
        int currentPos = 0;

        while (currentPos < lineProcessor.length()) {
            int startMultiCommentPos = lineProcessor.indexOf("/*", currentPos);
            int startInlineCommentPos = lineProcessor.indexOf("--", currentPos);

            // Determine which comment type comes first (if any)
            int firstCommentPos = -1;
            boolean isMultiLine = false;

            if (startMultiCommentPos != -1 && (startInlineCommentPos == -1 || startMultiCommentPos < startInlineCommentPos)) {
                firstCommentPos = startMultiCommentPos;
                isMultiLine = true;
            } else if (startInlineCommentPos != -1) {
                firstCommentPos = startInlineCommentPos;
                isMultiLine = false;
            }


            String partBeforeComment;
            if (firstCommentPos == -1) {

                partBeforeComment = lineProcessor.substring(currentPos);
                currentPos = lineProcessor.length();
            } else {

                partBeforeComment = lineProcessor.substring(currentPos, firstCommentPos);
                currentPos = firstCommentPos;
            }


            String effectivePart = partBeforeComment.trim();
            if (!effectivePart.isEmpty()) {
                appendAndCheckStatementEnd(effectivePart, state, statements);
            }

            if (firstCommentPos != -1) {
                if (isMultiLine) {
                    // Found /*
                    int endMultiCommentPos = lineProcessor.indexOf("*/", currentPos + 2);
                    if (endMultiCommentPos == -1) {

                        state.inMultilineComment = true;
                        currentPos = lineProcessor.length();
                    } else {

                        currentPos = endMultiCommentPos + 2;
                    }
                } else {

                    currentPos = lineProcessor.length();
                }
            }

        }
    }


    /**
     * Appends the effective line content to the current statement buffer
     * and checks if the line terminates the statement.
     */
    private static void appendAndCheckStatementEnd(String effectiveContent, ParseState state, List<String> statements) {
        // Append with a space if buffer is not empty and doesn't end with newline/space
        if (state.currentStatement.length() > 0 && !Character.isWhitespace(state.currentStatement.charAt(state.currentStatement.length() - 1))) {
            state.currentStatement.append(" ");
        }
        state.currentStatement.append(effectiveContent); //.append("\n"); // Append content, maybe add space instead of newline

        if (effectiveContent.endsWith(";")) {
            addStatementToList(statements, state);
        }
    }

    /**
     * Finalizes the current statement buffer, adds it to the list if not empty,
     * and clears the buffer.
     */
    private static void addStatementToList(List<String> statements, ParseState state) {
        String stmt = state.currentStatement.toString().trim();
        // Ensure it actually ends with a semicolon before removing
        if (stmt.endsWith(";")) {
            stmt = stmt.substring(0, stmt.length() - 1).trim();
        }
        if (!stmt.isEmpty()) {
            statements.add(stmt);
        }
        state.currentStatement.setLength(0); // Reset for the next statement
    }

    /**
     * Adds the final content remaining in the buffer after processing all lines.
     */
    private static void addFinalStatement(List<String> statements, ParseState state) {
        if (state.currentStatement.length() > 0) {
            String lastStmt = state.currentStatement.toString().trim();
            if (!lastStmt.isEmpty()) {

                if (lastStmt.endsWith(";")) {
                    lastStmt = lastStmt.substring(0, lastStmt.length() - 1).trim();
                }
                if (!lastStmt.isEmpty()) {
                    statements.add(lastStmt);
                }
            }
            state.currentStatement.setLength(0); // Clear buffer finally
        }
    }
}